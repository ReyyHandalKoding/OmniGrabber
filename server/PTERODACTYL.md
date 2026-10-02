# Pterodactyl setup

## Files
Upload the contents of `server/` to the Pterodactyl server root:
- `index.js`
- `package.json`
- `pterodactyl-start.sh`

## Startup command
```sh
npm start
```

`npm start` automatically runs `pterodactyl-start.sh`, which installs/updates the local yt-dlp binary when needed and then starts the API.

The script downloads the standalone `yt-dlp` binary into `./bin/yt-dlp` on first start. No root/sudo is required. It then starts the Node API.

## Allocation / port
Create one Pterodactyl allocation and expose that port publicly. Set `PORT` to the allocated port, or let the server template provide it. The process listens on `0.0.0.0`.

For the current prototype APK, the configured backend is:
`http://node-michie.jkt48node.id:3048`

For a production release, put HTTPS in front of this server with a reverse proxy/domain and then change `OMNI_BACKEND_URL`.

## Environment variables
Recommended:
- `PORT` = Pterodactyl allocated port
- `MAX_FILE_BYTES` = `209715200`
- `MAX_DURATION_SECONDS` = `600`
- `MAX_QUEUE` = `20`
- `WORKERS` = `1`
- `RATE_MAX` = `10`
- `JOB_TTL_MS` = `1800000`
- `YTDLP_PATH` = `$PWD/bin/yt-dlp` (the startup script sets this automatically)
- `PUBLIC_BASE_URL` = your public HTTPS base URL, e.g. `https://download.example.com`

Optional:
- `YTDLP_FORMAT` = `best[ext=mp4]/best`

## Test
After startup:
`GET https://YOUR-DOMAIN/api/health`

Expected shape:
```json
{"ok":true,"service":"omnigrabber-api"}
```
