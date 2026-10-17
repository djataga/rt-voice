# RT Voice

**RT Voice** is an open, rebuilt Android client for the RT news service — a
reconstruction of the retired **RT News** app (removed from Google Play,
last updated 2022) with a **voice reader** that reads articles aloud.

> Not affiliated with or endorsed by RT. News content is delivered by the same
> mobile API the original app used; all trademarks belong to their respective
> owners. Built for personal use.

## Features

- **News feed** — Latest, World News, Business and more, straight from the
  original app's section catalog (`news` endpoint).
- **Six language editions** — English, Français, Deutsch, Español, العربية,
  Русский. All API hosts were recovered from the original APK and verified live.
- **Article view** — the same HTML document the original rendered in a WebView,
  with pull-to-refresh.
- **🔊 Voice reader** — reads any article aloud with the system TTS engine:
  play / pause, skip paragraph forward/back, paragraph progress (`4 of 12`),
  and speed control (0.75× – 2×). The voice language follows the selected
  edition; audio focus is handled properly.
- **🎧 Background listening** — playback continues after you leave the app via
  a foreground media-playback service, with a media notification (play / pause,
  skip paragraph, stop), lock-screen controls and a persistent session.
- **📺 Shows** — the full program catalog of each edition; episode lists with
  direct MP4 playback (media3 / ExoPlayer).
- **🔴 Live TV** — the 24/7 RT and RTD channels streamed over HLS.
- **📣 Social tab** — RT's official accounts (Telegram, X, Rumble, Odysee,
  Facebook) plus a native post feed built from RT's public RSS mirrors
  (per-edition, with sections on the English edition). Posts open in the
  in-app reader with voice support via the article id embedded in the feed.
  X and Facebook are opened on their official pages — X is pay-per-use API
  only and Facebook requires page-admin access (and banned RT in 2024), so
  there is no free way to read them programmatically.
- **Bookmarks with offline listening** — a bookmark stores the article HTML, so
  saved articles can be read *and* listened to without a connection.
- **Share**, Material 3 UI, dark-mode aware, RTL support.

## Building

Requirements: Android Studio (or JDK 17+ with the Android SDK), API 34 platform.

```bash
./gradlew :app:assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio and press Run.

## Where the code came from

The original `RT_News_base.apk` was reverse engineered (apktool + jadx) to
recover its architecture, API contract and resources. The findings are written
up in [docs/REVERSE_ENGINEERING.md](docs/REVERSE_ENGINEERING.md). This
repository contains **no original proprietary code** — it is a clean-room Kotlin
reimplementation of the app's core reading experience, plus the new voice reader.

The original APK and decompiled output are expected in `OriginApp/` and
`decompiled/` locally for reference; both are git-ignored and not distributed.

## API notes

The original app talked to `https://eng.mobileapiru.com/rtmobile/` (and
per-edition siblings). As of October 2026 every edition is still live:

| Edition | Base URL |
|---|---|
| English | `https://eng.mobileapiru.com/rtmobile/` |
| Français | `https://fra.mobileapiru.com/rtmobile/v3/` |
| Deutsch | `https://ger.mobileapiru.com/rtmobile/` |
| Español | `https://eps.mobileapiru.com/mobiledata/v3/` |
| العربية | `https://arb.mobileapiru.com/rtmobile3/` |
| Русский | `https://rus.mobileapiru.com/api/mobile/` |

Key endpoints (identical contract on all editions, JSON envelope `{"data": ...}`):

- `GET news` — section catalog
- `GET news/latest` — latest articles
- `GET news/{section}` — section feed
- `GET news/latest/{id}/html` — full article HTML
- `GET shows` + `GET shows/{id}/episodes` — program catalog and episodes
  (episodes carry direct MP4 `video.url`s)
- `GET video` — 24/7 live channels with HLS playlists

Article HTML uses stable semantic anchors (`#title`, `.app-mobile__subtitle`,
`#content`) which the voice reader uses for clean text extraction.

## Roadmap ideas

- Media-session integration for the video player (background video)
- Web-page highlight sync while reading aloud
- Galleries (`media`, galleries endpoints still work)
- Podcasts (`podcasts`, `soundcloud` endpoints)

## License

[MIT](LICENSE) for the code in this repository. News content and the RT brand
remain the property of their respective owners.
