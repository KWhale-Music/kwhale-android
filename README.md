# KWhale — Android-клиент

Переосмысление музыкального плеера на современном стеке: **Jetpack Compose + Material 3**, в связке с self-hosted музыкальным сервером **KWhale v3** (FastAPI + PostgreSQL, 9 внешних источников).

> UI — **Jetpack Compose + Material 3** (полный переход с Views/XML завершён). Тема: DARK / LIGHT / AMOLED / Monet + 129 акцентов.

## Стек

- Kotlin 2.1 · minSdk 31 · targetSdk 35
- Jetpack Compose + Material 3 · Navigation Compose
- AndroidX Media3 (ExoPlayer + MediaSession) — фоновое воспроизведение
- OkHttp + kotlinx.serialization · Coil (обложки) · WorkManager (загрузки) · Glance (виджет)

## Модули

```
app/            — Compose-экраны, плеер (Media3), загрузки, виджет, WorkManager
core:model      — доменные модели (Track, Album, Artist, Playlist)
core:design     — KwhaleTheme (M3), 129 акцентов, VK Sans
core:network    — KwhaleApi, ApiClient (single-origin), TokenStore, DTO
core:player     — PlaybackState (состояние плеера)
api/            — OpenAPI 3.1 контракт KWhale Core v1 (+ mock)
```

## Архитектура

- **Single-origin**: все запросы (стрим/обложки/тексты) идут через один API-origin; `HostPolicyInterceptor` блокирует уход на сторонние хосты.
- **Auth**: Bearer JWT с refresh-on-401; токены в `EncryptedSharedPreferences`.
- **Плеер**: `Media3` `MediaSessionService` — фон, шторка уведомлений, lock-screen.
- **Оффлайн**: загрузки через WorkManager; воспроизведение по локальному файлу (устойчиво к смене URL сервера).

## Сборка

```bash
# окружение
export JAVA_HOME=<jdk-17> ANDROID_HOME=<android-sdk>

# debug
./gradlew :app:assembleDebug
# release (R8 + resource-shrink → ~11 МБ APK)
./gradlew :app:assembleRelease

# тесты сетевого слоя
./gradlew :core:network:testDebugUnitTest
```

## Конфигурация

- `API_BASE_URL` задаётся в `app/build.gradle.kts` через `buildConfigField`:
  - debug → `http://127.0.0.1:19001/v1` (через `adb reverse tcp:19001 tcp:19001`)
  - release → `https://music.dueattendant149.org/v1`
- Сетевая безопасность (`res/xml/network_security_config.xml`): cleartext разрешён только для локального dev-сервера (`127.0.0.1`, `10.0.2.2`); публичный API — TLS.

## Сервер

Клиент работает в паре с сервером **KWhale v3** (`../kwhale-v3`): агрегированный поиск по 9 источникам, дедупликация, стриминг через единый `/v1`, библиотека/плейлисты/рекомендации. Контракт — `api/openapi.yaml` (KWhale Core v1).

## Mock API

Для разработки UI без сервера — `api/mock` (Prism) отдаёт примеры из `openapi.yaml`. Точка входа мока и настройка base URL — в `ApiConfig`.
