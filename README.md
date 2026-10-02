# OmniGrabber

OmniGrabber now uses a **Pterodactyl backend** for extraction instead of RapidAPI/Cobalt/direct in-app extraction.

## Architecture
`Android APK → HTTPS → Pterodactyl API → queue/worker → yt-dlp → temporary file → APK download`

The existing Android UI is retained. The TikTok flow now submits a job, polls `/api/jobs/:id`, then previews/downloads `/api/jobs/:id/result`.

## One-time setup
1. Deploy `server/` to Pterodactyl using `server/PTERODACTYL.md`.
2. Get its HTTPS public URL.
3. Set it before building:
```sh
./set_backend_url.sh https://your-backend.example
```
4. Commit/push and let GitHub Actions build `OmniGrabber.apk`.

No RapidAPI secret is required.


## Current Pterodactyl backend
- URL: `http://node-michie.jkt48node.id:3048`
- Health: `/api/health`
- `npm start` bootstraps yt-dlp automatically through `pterodactyl-start.sh`.
