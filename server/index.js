const express = require('express');
const fs = require('fs');
const fsp = fs.promises;
const path = require('path');
const crypto = require('crypto');
const { spawn } = require('child_process');

const app = express();
app.use(express.json({ limit: '32kb' }));

const PORT = Number(process.env.PORT || 3000);
const HOST = process.env.HOST || '0.0.0.0';
const TMP_DIR = path.resolve(process.env.TMP_DIR || path.join(__dirname, 'tmp'));
const YTDLP = process.env.YTDLP_PATH || 'yt-dlp';
const MAX_FILE_BYTES = Number(process.env.MAX_FILE_BYTES || 200 * 1024 * 1024);
const MAX_DURATION = Number(process.env.MAX_DURATION_SECONDS || 600);
const JOB_TTL_MS = Number(process.env.JOB_TTL_MS || 30 * 60 * 1000);
const MAX_QUEUE = Number(process.env.MAX_QUEUE || 20);
const WORKERS = Math.max(1, Number(process.env.WORKERS || 1));
const PUBLIC_BASE_URL = (process.env.PUBLIC_BASE_URL || '').replace(/\/$/, '');
const RATE_WINDOW_MS = 60_000;
const RATE_MAX = Number(process.env.RATE_MAX || 10);
const rateMap = new Map();
const jobs = new Map();
const queue = [];
let activeWorkers = 0;

const ALLOWED_HOSTS = [
  /(^|\.)tiktok\.com$/i,
  /(^|\.)youtube\.com$/i,
  /(^|\.)youtu\.be$/i,
  /(^|\.)instagram\.com$/i,
  /(^|\.)facebook\.com$/i,
  /(^|\.)fb\.watch$/i,
  /(^|\.)x\.com$/i,
  /(^|\.)twitter\.com$/i
];

function clientIp(req) {
  const raw = req.headers['x-forwarded-for'];
  return String(raw || req.socket.remoteAddress || 'unknown').split(',')[0].trim();
}

function rateLimit(req, res, next) {
  const ip = clientIp(req);
  const now = Date.now();
  const entry = rateMap.get(ip) || { start: now, count: 0 };
  if (now - entry.start >= RATE_WINDOW_MS) { entry.start = now; entry.count = 0; }
  entry.count++;
  rateMap.set(ip, entry);
  if (entry.count > RATE_MAX) return res.status(429).json({ error: 'Rate limit exceeded. Try again later.' });
  next();
}

function parseUrl(value) {
  if (typeof value !== 'string' || value.length < 8 || value.length > 2048) throw new Error('Invalid URL');
  let u;
  try { u = new URL(value); } catch { throw new Error('Invalid URL'); }
  if (!['https:', 'http:'].includes(u.protocol)) throw new Error('Only HTTP(S) URLs are allowed');
  if (u.username || u.password) throw new Error('Credentials in URL are not allowed');
  if (!ALLOWED_HOSTS.some(re => re.test(u.hostname))) throw new Error('Unsupported platform');
  return u.toString();
}

function platformFor(url) {
  const h = new URL(url).hostname.toLowerCase();
  if (h.endsWith('tiktok.com')) return 'tiktok';
  if (h === 'youtu.be' || h.endsWith('youtube.com')) return 'youtube';
  if (h.endsWith('instagram.com')) return 'instagram';
  if (h.endsWith('facebook.com') || h === 'fb.watch') return 'facebook';
  return 'x';
}

function newJob(url) {
  const id = crypto.randomUUID();
  return { id, url, platform: platformFor(url), status: 'queued', progress: 0, createdAt: Date.now(), updatedAt: Date.now(), title: null, thumbnail: null, duration: null, formats: [], file: null, error: null, process: null };
}

function runProcess(args, job, onLine) {
  return new Promise((resolve, reject) => {
    const child = spawn(YTDLP, args, { cwd: TMP_DIR, shell: false, windowsHide: true });
    job.process = child;
    let stdout = '', stderr = '';
    child.stdout.on('data', b => { const s = b.toString(); stdout += s; s.split(/\r?\n/).forEach(onLine); });
    child.stderr.on('data', b => { const s = b.toString(); stderr += s; s.split(/\r?\n/).forEach(onLine); });
    child.on('error', err => reject(err));
    child.on('close', code => code === 0 ? resolve({ stdout, stderr }) : reject(new Error((stderr || stdout || `yt-dlp exited ${code}`).trim().slice(-1800))));
  });
}

function safeFileName(s) {
  return String(s || 'media').replace(/[\\/:*?"<>|\x00-\x1F]/g, '_').replace(/\s+/g, ' ').trim().slice(0, 100) || 'media';
}

async function processJob(job) {
  const dir = path.join(TMP_DIR, job.id);
  await fsp.mkdir(dir, { recursive: true });
  try {
    job.status = 'processing'; job.progress = 3; job.updatedAt = Date.now();
    const meta = await runProcess([
      '--dump-single-json', '--skip-download', '--no-playlist',
      '--socket-timeout', '20', '--retries', '2', '--no-warnings', job.url
    ], job, () => {});
    let info;
    try { info = JSON.parse(meta.stdout.trim().split(/\n(?=\{)/).pop()); }
    catch { throw new Error('Extractor returned invalid metadata'); }

    const duration = Number(info.duration || 0);
    if (duration && duration > MAX_DURATION) throw new Error(`Duration exceeds ${MAX_DURATION} seconds`);
    job.title = info.title || info.fulltitle || 'OmniGrabber media';
    job.thumbnail = typeof info.thumbnail === 'string' ? info.thumbnail : null;
    job.duration = duration || null;
    job.formats = Array.isArray(info.formats) ? info.formats.filter(f => f && (f.url || f.format_id)).slice(0, 40).map(f => ({ id: String(f.format_id || ''), ext: f.ext || null, height: f.height || null, width: f.width || null, fps: f.fps || null, filesize: f.filesize || f.filesize_approx || null, audio: !!f.acodec && f.acodec !== 'none', video: !!f.vcodec && f.vcodec !== 'none' })) : [];
    job.progress = 12; job.updatedAt = Date.now();

    const outTemplate = path.join(dir, '%(title).100s.%(ext)s');
    const format = process.env.YTDLP_FORMAT || 'best[ext=mp4]/best';
    const args = [
      '--newline', '--no-playlist', '--max-filesize', String(MAX_FILE_BYTES),
      '--socket-timeout', '20', '--retries', '2', '--no-warnings',
      '-f', format, '-o', outTemplate, job.url
    ];
    await runProcess(args, job, line => {
      const m = line.match(/(\d+(?:\.\d+)?)%/);
      if (m) job.progress = Math.max(12, Math.min(96, Math.round(Number(m[1]) * 0.84 + 12)));
      job.updatedAt = Date.now();
    });

    const files = (await fsp.readdir(dir)).filter(x => !x.endsWith('.part') && !x.endsWith('.ytdl'));
    if (!files.length) throw new Error('Extractor finished without a media file');
    const full = path.join(dir, files[0]);
    const st = await fsp.stat(full);
    if (st.size <= 0) throw new Error('Downloaded file is empty');
    if (st.size > MAX_FILE_BYTES) throw new Error('Downloaded file exceeds server limit');
    job.file = { path: full, name: safeFileName(path.basename(full)), size: st.size, mime: mimeFor(path.extname(full)) };
    job.status = 'completed'; job.progress = 100; job.updatedAt = Date.now();
  } catch (err) {
    if (err && err.code === 'ENOENT') job.error = 'yt-dlp is not installed on the server';
    else job.error = String(err && err.message || err).slice(-1800);
    job.status = 'failed'; job.updatedAt = Date.now();
  } finally {
    job.process = null;
    activeWorkers--;
    pump();
  }
}

function mimeFor(ext) {
  ext = ext.toLowerCase();
  if (ext === '.mp4') return 'video/mp4';
  if (ext === '.webm') return 'video/webm';
  if (ext === '.m4a') return 'audio/mp4';
  if (ext === '.mp3') return 'audio/mpeg';
  if (ext === '.jpg' || ext === '.jpeg') return 'image/jpeg';
  if (ext === '.png') return 'image/png';
  return 'application/octet-stream';
}

function pump() {
  while (activeWorkers < WORKERS && queue.length) {
    const job = queue.shift();
    activeWorkers++;
    processJob(job);
  }
}

function publicJob(job, req) {
  const base = PUBLIC_BASE_URL || `${req.protocol}://${req.get('host')}`;
  const result = job.status === 'completed' ? `${base}/api/jobs/${job.id}/result` : null;
  return { jobId: job.id, status: job.status, progress: job.progress, platform: job.platform, title: job.title, thumbnail: job.thumbnail, duration: job.duration, formats: job.formats, downloadUrl: result, error: job.error };
}

app.get('/api/health', (req, res) => res.json({ ok: true, service: 'omnigrabber-api', workers: WORKERS, active: activeWorkers, queue: queue.length }));
app.post('/api/jobs', rateLimit, (req, res) => {
  try {
    const url = parseUrl(req.body && req.body.url);
    if (queue.length + activeWorkers >= MAX_QUEUE) return res.status(429).json({ error: 'Server queue is full' });
    const job = newJob(url);
    jobs.set(job.id, job); queue.push(job); pump();
    res.status(202).json(publicJob(job, req));
  } catch (e) { res.status(400).json({ error: e.message || 'Invalid request' }); }
});

app.get('/api/jobs/:id', rateLimit, (req, res) => {
  const job = jobs.get(req.params.id);
  if (!job) return res.status(404).json({ error: 'Job not found' });
  res.json(publicJob(job, req));
});

app.get('/api/jobs/:id/result', rateLimit, async (req, res) => {
  const job = jobs.get(req.params.id);
  if (!job) return res.status(404).json({ error: 'Job not found' });
  if (job.status !== 'completed' || !job.file) return res.status(409).json({ error: 'Result not ready' });
  try {
    await fsp.access(job.file.path, fs.constants.R_OK);
    res.setHeader('Content-Type', job.file.mime);
    res.setHeader('Content-Disposition', `attachment; filename*=UTF-8''${encodeURIComponent(job.file.name)}`);
    res.setHeader('Content-Length', String(job.file.size));
    res.sendFile(job.file.path);
  } catch { res.status(410).json({ error: 'Temporary result expired' }); }
});

app.use((err, req, res, next) => { console.error(err); res.status(500).json({ error: 'Internal server error' }); });

async function cleanup() {
  const cutoff = Date.now() - JOB_TTL_MS;
  for (const [id, job] of jobs) {
    if (job.updatedAt < cutoff && !job.process) {
      jobs.delete(id);
      try { await fsp.rm(path.join(TMP_DIR, id), { recursive: true, force: true }); } catch {}
    }
  }
  for (const [ip, e] of rateMap) if (Date.now() - e.start > RATE_WINDOW_MS * 2) rateMap.delete(ip);
}

(async () => {
  await fsp.mkdir(TMP_DIR, { recursive: true });
  app.listen(PORT, HOST, () => console.log(`OmniGrabber API listening on ${HOST}:${PORT}`));
  setInterval(cleanup, 60_000).unref();
})();
