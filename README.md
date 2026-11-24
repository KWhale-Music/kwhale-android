<div align="center">

**English** | [Русский](README.ru.md)

<br/>

<img src="docs/images/cover.png" alt="KWhale for Android" width="100%"/>

# KWhale for Android

**A modern music player for your self-hosted music server.**

[![License: AGPL-3.0](https://img.shields.io/badge/license-AGPL--3.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![minSdk](https://img.shields.io/badge/minSdk-31-green.svg)](#)

</div>

Native Android client for the [KWhale Server](https://github.com/kwhale-music/kwhale-server),
rebuilt from the ground up on **Jetpack Compose + Material 3**. Full theme system
(dark / light / AMOLED / Monet + 129 accents), background playback via Media3,
offline downloads, home-screen widget, and share links that open right into the app.

## ✨ Features

- **Material 3 everything** — dynamic color (Monet), AMOLED true-black, 129 hand-picked
  accents, dark / light / system modes.
- **Playback** — AndroidX Media3 `MediaSessionService`: background playback, media
  notification, lock-screen controls, queue, lyrics view.
- **Offline-first downloads** — WorkManager-based, plays local files even when the
  server URL changes (files are keyed by remote ID, not URL).
- **Home-screen widget** — Glance-based mini player following the M3 theme.
- **Sharing** — share tracks/albums/artists, `kwhale://track|album|artist|share` deep links,
  shared-track bottom sheet.
- **Security** — single-origin API client (`HostPolicyInterceptor` blocks third-party hosts),
  Bearer JWT with refresh-on-401 that survives network blips, tokens in
  `EncryptedSharedPreferences`, cleartext only for loopback / debug builds.

## 📸 Screenshots

<div align="center">
<img src="docs/images/screens-main.png" alt="Home, Search, Library, Profile, Album, Artist" width="100%"/>
<br/>
<img src="docs/images/screens-player.png" alt="Player, Queue, Lyrics, Playlist, Settings, Appearance" width="100%"/>
</div>

## 🚀 Quickstart

```bash
git clone https://github.com/kwhale-music/kwhale-android.git
cd kwhale-android
export JAVA_HOME=<jdk-17> ANDROID_HOME=<android-sdk>

# debug build
./gradlew :app:assembleDebug

# release build (R8 + resource shrinking → ~11 MB APK)
./gradlew :app:assembleRelease
```

Prebuilt APKs are attached to [GitHub Releases](https://github.com/kwhale-music/kwhale-android/releases).

Point the app at your server: **Settings → server URL**, or set the debug build's default in
(gitignored) `local.properties`:

```properties
API_BASE_URL=http://<your-lan-host>:19001/v1
```

The release build defaults to `https://music.dueattendant149.org/v1` and can be switched
to any KWhale instance in the UI.

## ⚙️ Configuration

| Item | Where | Notes |
|---|---|---|
| Debug API URL | `API_BASE_URL` in `local.properties` | default `http://10.0.2.2:19001/v1` (emulator) |
| Release API URL | in-app server picker | default `https://music.dueattendant149.org/v1` |
| Release signing | `APPS_*` in `~/.gradle/gradle.properties` | standard release keystore setup |
| Network security | `res/xml/network_security_config.xml` | HTTPS-only in release; debug builds allow cleartext for LAN dev hosts |

## 🏗 Architecture

```
app/            — Compose screens, player (Media3), downloads, widget, WorkManager
core:model      — domain models (Track, Album, Artist, Playlist)
core:design     — KwhaleTheme (M3), 129 accents, fonts
core:network    — KwhaleApi, ApiClient (single-origin), TokenStore, DTOs
core:player     — PlaybackState
api/            — OpenAPI 3.1 contract of KWhale Core v1 (+ mock server)
```

## 🧭 Author's review

*Honest notes from the author — this section is a draft and will keep evolving.*

What I'm genuinely happy with:

- **The theme system.** 129 accents, AMOLED and Monet in one coherent `core:design` —
  it's the part of the app that feels finished and personal.
- **Network resilience.** Refresh-on-401 keeps the session across network failures
  (cleared only on a genuinely invalid token), and the single-origin policy means the app
  physically can't leak streams to third-party hosts.
- **Offline robustness.** Keying downloads by remote ID instead of URL means switching
  servers doesn't break a downloaded library.

What's honestly still rough:

- **Silent UI failures.** Some actions fail without feedback (optimistic state without
  rollback, follow-button no-ops). The error-handling pass is the top priority.
- **Pagination is uneven** — some lists claim totals they don't have. Honest totals are
  coming.
- **Lint debt** — a known set of warnings (`@OptIn`, foreground service types) is still
  open. Not dangerous, just untidy.

Next up: error-handling honesty pass, download progress/retry UX, lint → 0, and a
proper release profile (versioning, reproducible signing docs).

## 🗺 Status

Beta — version `0.1.22`, used daily by the author on a physical device.

## 📄 License

[AGPL-3.0](LICENSE) — same terms as the server.

Third-party notes: the UI was designed as a *recreation* — code is original, but the
font set (VK Sans) and some visual language reference VK X and remain the property of
their owners; treat them as replaceable design assets. Streaming connectors live in the
server repository and integrate third-party services under their own terms.
