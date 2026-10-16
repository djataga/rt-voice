# Reverse-Engineering Notes: RT News 3.5.60

Analysis of `OriginApp/RT_News_base.apk` (34.5 MB, May 2022), produced with
**apktool 2.10.0** (resources/manifest/smali) and **jadx 1.5.1** (Java sources).
This document records what the original app is made of; the code in this
repository is a clean-room rebuild of the parts that matter, plus a new voice
reader. No original code is redistributed here.

## Identity

| Field | Value |
|---|---|
| Package | `com.rt.mobile.english` |
| Version | 3.5.60 (versionCode 174) |
| Built with | compileSdk 31 (Android 12) |
| min / target SDK | 19 (Android 4.4) / 30 |
| Launch activity | `com.rt.mobile.english.ui.MainActivity` |
| Deep-link schemes | `rtnews://`, `frenchrtnews://`, `arabicrtnews://`, `spanishrtnews://`, `russianrtnews://`, `deutschrtnews://` |

## Components (from the decoded manifest)

- `RTApp` — application class (Dagger `AppComponent`)
- `MainActivity` (launcher), `ArticleActivity`, `BookmarkActivity`,
  `EditionActivity`, `SettingsActivity`, `AboutActivity`, `GalleryPhotosActivity`,
  `EpisodesActivity`, `ShowEpisodeActivity`, `ChromecastActivity`,
  `ExoPlayerActivity`
- `FCMService` — push notifications
- `WearableMessageListenerService` — Wear OS companion (micro APK bundled in
  `res/raw/android_wear_micro_apk.apk`)

## Third-party stack (~30 SDKs, mostly dead weight for a personal rebuild)

Retrofit + OkHttp + Gson, Dagger, ButterKnife, Kotlin, Joda-Time, Picasso,
ExoPlayer + Cast, WebRTC (`libjingle_peerconnection_so.so`, live streams),
Octoshape + Streamroot DNA (P2P video), Firebase (FCM/Analytics),
Fabric/Crashlytics, Google Analytics (legacy), Google Ads + Taboola (ads),
Facebook SDK, Adjust, Comscore, Sentry, XLog, KeepSafe (Realm?).

None of these are required to read news; the ads/analytics/DRM/P2P stack is
what made a faithful recompile infeasible and is intentionally not rebuilt.

## The API (the valuable part)

Base URL resolution (`ApiAvailabilityServiceImpl`): hosts come from
`R.array.hosts` (per language), are probed via `<host>healthcheck` expecting
HTTP 200, the winner is cached in SharedPreferences, and a localized
`R.string.api_path` (empty in EN) is appended.

### Hosts (recovered from `res/values-*/arrays.xml`, verified live 2026-10)

| Edition | Host | Status |
|---|---|---|
| en | `https://eng.mobileapiru.com/rtmobile/` | 200 |
| ru | `https://rus.mobileapiru.com/api/mobile/` | 301 → `https://russiatoday.ru/api/mobile/` (200) |
| fr | `https://fra.mobileapiru.com/rtmobile/v3/` | 301 → `https://rtenfrancais.tv/rtmobile/v3/` |
| es | `https://eps.mobileapiru.com/mobiledata/v3/` | 200 |
| ar | `https://arb.mobileapiru.com/rtmobile3/` | 200 |
| de | `https://ger.mobileapiru.com/rtmobile/` | 200 |

### Endpoints (`ArticlesService`, `HtmlService`, ... Retrofit interfaces)

All responses share the envelope `{"data": <T>}` (decompiled class `Message<T>`).

| Call | Purpose |
|---|---|
| `GET news` | section catalog: `[{id, title}]` |
| `GET news/latest` | latest articles |
| `GET news/{section}` | section feed |
| `GET news/breaking` | breaking banner |
| `GET news/latest/{id}` | single article meta |
| `GET news/latest/{id}/html` | **article body** as a complete HTML document |
| `GET posts`, `posts/{id}`, `posts/{id}/html` | opinion pieces |
| `GET news/viral`, `news/actualidad` | viral / Spanish-only feeds |
| `GET othernews...` | secondary sections |
| `GET shows...`, `inmotion/...`, `rtonline/...`, `rtplay/...` | shows & video clubs |

### Models

`Article`: `id` (number, coerced to string), `title`, `summary`, `image`,
`imageTitle`, `section`, `sectionTitle`, `time` (**format `yyyyMMdd'T'HHmmss`, UTC**,
e.g. `20261016T040836`), `url` (web URL), `type`, `images[]`,
`video {url, youtube_id, size, mute}`.

## How articles are rendered

`ArticleFragment` fetches `news/latest/{id}/html` (Retrofit scalars) and calls

```java
webView.loadDataWithBaseURL(base + "news/latest/{id}/html/", body, "text/html", "utf-8", null);
```

The HTML is a self-contained document with CDN CSS/JS and **stable semantic
anchors** — this is what makes the voice reader's text extraction reliable:

```html
<div class="app-mobile" id="app-mobile">
  <div class="app-mobile__rubric" id="section">World News</div>
  <h1 class="app-mobile__title" id="title">…</h1>
  <time id="date" datetime="16 Oct 2026" class="app-mobile__date">Published: …</time>
  <p class="app-mobile__subtitle">…lead…</p>
  <div class="app-mobile__media" id="cover">…image…</div>
  <div class="app-mobile__text" id="content">
    <p>…</p><p>…</p>
  </div>
</div>
```

## Bookmarks

`DatabaseManager` (plain SQLite, database `rt_voice` equivalent: original name
unimportant) — table `bookmarks(article_id TEXT PK, item TEXT /*article JSON*/,
html TEXT, ...)`. Storing the HTML gives offline reading; RT Voice additionally
stores the edition code so the offline voice keeps the right language.

## Other behaviours worth keeping

- Sections tabs ("Latest", "World News", ...) with a drawer/toolbar for editions
- Swipe-to-refresh everywhere; error + retry states
- External links opened outside the WebView; API-relative links kept inside

## What was intentionally *not* rebuilt

Push notifications (FCM project gone), Chromecast, Wear OS, WebRTC live TV,
P2P video, ads/analytics/crash reporting (all keyless/dead), galleries & shows
(possible later — endpoints still work), settings (edition picker lives in the
toolbar now).
