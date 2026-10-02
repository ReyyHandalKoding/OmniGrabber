#!/bin/sh
set -eu
mkdir -p bin tmp
if [ ! -x ./bin/yt-dlp ]; then
  echo '[OmniGrabber] Installing yt-dlp...'
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o ./bin/yt-dlp
  elif command -v wget >/dev/null 2>&1; then
    wget -q https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -O ./bin/yt-dlp
  else
    echo 'curl/wget is required in the Pterodactyl image.' >&2
    exit 1
  fi
  chmod +x ./bin/yt-dlp
fi
export YTDLP_PATH="${YTDLP_PATH:-$PWD/bin/yt-dlp}"
if [ ! -d ./node_modules/express ]; then
  echo '[OmniGrabber] Installing Node dependencies...'
  npm install --omit=dev
fi
exec node index.js
