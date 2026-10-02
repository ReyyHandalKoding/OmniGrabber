# OmniGrabber Pterodactyl API

Backend queue for OmniGrabber. It uses yt-dlp as the extractor and keeps temporary files under `server/tmp`.

## Pterodactyl
Use a Node.js 22 image. Upload this `server` folder as the server root, or run from the repository root with `cd server`.

Startup:
`npm install --omit=dev && node index.js`

Environment:
- `PORT`: allocated Pterodactyl port (default 3000)
- `YTDLP_PATH`: default `yt-dlp`
- `MAX_FILE_BYTES`: default 209715200 (200 MB)
- `MAX_DURATION_SECONDS`: default 600
- `MAX_QUEUE`: default 20
- `WORKERS`: default 1
- `RATE_MAX`: default 10 requests/minute/IP
- `JOB_TTL_MS`: default 1800000 (30 min)

Endpoints:
- `GET /api/health`
- `POST /api/jobs` body `{ "url": "https://..." }`
- `GET /api/jobs/:id`
- `GET /api/jobs/:id/result`

Do not expose the server over plain HTTP to the public internet. Put it behind HTTPS/reverse proxy if the Pterodactyl allocation itself is not TLS.
