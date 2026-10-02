#!/data/data/com.termux/files/usr/bin/bash
set -e
if [ -z "${1:-}" ]; then
  echo "Usage: ./set_backend_url.sh https://your-backend.example"
  exit 1
fi
URL="$1"
case "$URL" in
  https://*) ;;
  *) echo "Backend URL wajib HTTPS"; exit 1;;
esac
FILE="gradle.properties"
if grep -q '^OMNI_BACKEND_URL=' "$FILE"; then
  sed -i "s#^OMNI_BACKEND_URL=.*#OMNI_BACKEND_URL=$URL#" "$FILE"
else
  printf '\nOMNI_BACKEND_URL=%s\n' "$URL" >> "$FILE"
fi
echo "Backend URL set: $URL"
