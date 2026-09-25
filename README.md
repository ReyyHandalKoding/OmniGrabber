# OmniGrabber 1.6

TikTok-first Android downloader.

- Accepts full TikTok URLs and `vt.tiktok.com` short links.
- Resolves TikTok redirects inside the app.
- Uses the RapidAPI TikTok Post Details endpoint.
- Finds a direct video URL from the JSON response without depending on Cobalt or native yt-dlp.
- Preview and Download.
- Saves to `Download/OmniGrabber`.
- Background video + looping theme music.
- Preview pauses the theme music and closing preview resumes it.
- APK output: `OmniGrabber.apk`.

GitHub Actions reads the RapidAPI credential from the repository secret `RAPIDAPI_KEY`.
