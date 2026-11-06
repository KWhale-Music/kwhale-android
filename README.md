# vkx-recreate — пересоздание VK X (UI)

Пересоздание **VK X** (`ua.itaysonlab.vkx`) по декомпиляции APK.
UI — **Android Views + XML**, как в оригинале (кастомный `liquidnavigation` + Material Components);
Compose используется только для now-playing, как и в оригинале.

- RE-отчёт: [`docs/REPORT.md`](docs/REPORT.md)
- Native/anti-tamper: [`docs/NATIVE.md`](docs/NATIVE.md)
- Паритет: [`docs/PARITY.md`](docs/PARITY.md)
- Проверка спецификации: [`docs/SPEC_CHECK.md`](docs/SPEC_CHECK.md)
- API-контракт: [`api/openapi.yaml`](api/openapi.yaml), [`api/README.md`](api/README.md)
- APK: `app/build/outputs/apk/debug/app-debug.apk`
- Исходный артефакт и декод: `re/`

## Сборка

```bash
export JAVA_HOME=/var/home/dueattendant149/toolbox/jdk-17
export ANDROID_HOME=/var/home/dueattendant149/android-sdk
./gradlew :app:assembleDebug
```

JDK 17 обязателен: модули `core:model` и `core:player` используют
`jvmToolchain(17)`. JDK 21 для сборки Android-модулей тоже подходит, но
pure-Kotlin-модули требуют установленный тулчейн 17.

В этом окружении `/home` — симлинк на `/var/home`. Если Gradle падает с
`file ... is located outside the root directory`, запускайте сборку из
`/home/dueattendant149/vkx-recreate` (тот же проект, но единый путь).

## Стек

- Kotlin 2.1.0, AGP 8.7.3, Gradle 8.10.2, JDK 17.
- Android Views + XML, ViewBinding, Material Components, AppCompat, RecyclerView.
- Compose (только now-playing): BOM 2024.12.01, Material 3.
- minSdk 31, target/compile 35.
- Пакет `org.dueattendant149.vkx`, label «VK X».

## Структура

```
app                 — Activity, Views-экраны, кастомный NextDockView, Compose-плеер
core:model          — домен: Track, Album, Artist, Playlist, User
core:data           — VkxRepository (интерфейс), MockVkxRepository, MockCatalog,
                      LibraryStore (DataStore: лайки + пользовательские плейлисты)
core:design         — ресурсы VKX, Compose-тема для now-playing, VK Sans, 129 акцентов
core:player         — MockPlayer (симуляция очереди/прогресса)
core:network        — VkxApi, OkHttp, JWT-сессия, host-policy, DTO
api/                — OpenAPI 3.1, примеры, mock (Prism/WireMock)
re/                 — APK + decoded-ресурсы + полный JADX-дамп
docs/               — RE-отчёт, native, паритет
```

## Что реализовано

- **Каркас** по `activity_app.xml`: `liquid_container` + `bottom_sheet` (peek 61dp)
  с Compose-плеером + `NextDockView` 72dp.
- **Док**: 5 вкладок с реальными иконками VK X — Главная / Поиск / Радио / Библиотека / Меню.
- **Библиотека** по визуальному рефу: пилюли «Библиотека / Загружено», секция плейлистов
  с карточками 140dp, «Мои аудиозаписи» со счётчиком и сортировкой, «Перемешать»,
  трек-строки с тремя точками и бейджем explicit.
- **Главная-каталог**: баннер, action-ряды, полки альбомов/артистов/плейлистов, треки.
- **Поиск**: поле, «Популярные запросы», секции треков/альбомов/артистов/плейлистов.
- **Альбом / Артист / Плейлист**: детальные экраны с шапками, play/shuffle, треклистами.
- **Плейлисты**: создание, добавление трека из меню «⋮», удаление из своего плейлиста.
- **Меню**: профиль, история, ссылки, настройки библиотеки, оформление, о приложении, выход.
- **Темы**: 37 тем из `theme_catalog.json` + быстрые режимы Light/Dark/AMOLED/Monet.
- **Акценты**: полный каталог VK X — 129 акцентов, мгновенное применение.
- **Now-playing**: раскрывается bottom sheet из мини-плеера (без отдельной Activity),
  градиентный фон, крупная обложка, seek, shuffle/repeat, лайк, очередь-заглушка.
- **Мини-плеер** 61dp: лавандовый тинт, прогресс, ×, play/pause.
- **Реальный звук**: Media3 ExoPlayer (audio focus, becoming-noisy), локальные OGG-сэмплы
  `res/raw/sample_1..4`; треки маппятся на сэмплы до подключения сети.
- **Персистентность**: лайки и пользовательские плейлисты (DataStore).
- **Сеть**: `core:network` с `VkxApi`, JWT-сессией (access/refresh), host-policy
  (только один origin) и DTO под `api/openapi.yaml`.
- **Вход**: реальный экран email/password + регистрация; токены в
  `EncryptedSharedPreferences`; выход из меню.
- **Контракт**: OpenAPI 3.1 с рекомендательным слоем, медиа, изображениями,
  событиями и радио; Prism mock в `api/mock`.
- **Фоновое воспроизведение**: `PlaybackService` (MediaSessionService) +
  медиа-уведомление, `MediaController` в UI.
- **Скачивание/офлайн**: WorkManager, foreground-уведомление, локальный URI,
  секция «Скачано».
- **Детальные экраны**: удалённые album/artist/playlist через `VkxApi`,
  лайки, добавление/удаление из плейлиста, создание плейлиста.
- **Сигналы рекомендаций**: `EventTracker` отправляет play/skip/pause в `/events`.
- **Онбординг**: выбор жанров после первого входа.
- **Миксы**: `RemoteRadioView` — личные миксы; радио удалено из контракта до recommendation-модуля.
- **Тексты**: `LyricsSheet` + `/tracks/{id}/lyrics`.
- **Темы**: все 37 тем из `theme_catalog.json` + 129 акцентов.
- **Виджет**: один Glance-виджет плеера (play/pause, next, текущий трек).

## Пока заглушки / следующий этап

- Главная, поиск, библиотека и детальные экраны читают `VkxApi`; мок-каталог
  остаётся для старых mock-экранов и превью.
- Длительности в списке — метаданные; для скачанных/мок-треков возможна
  локальная длительность.
- «Загружено», история, ссылки — заглушки.
- Rename/delete плейлиста, drag&drop — не реализованы.
- Полноценный now-playing UI рекомендаций/радио и кастомизация плеера — следующие этапы.

## Проверка

```bash
./gradlew :app:assembleDebug
./gradlew :core:network:testDebugUnitTest
tools/validate-openapi.sh
```

- `assembleDebug` — зелёный.
- Сетевые тесты: login, refresh-on-401, host-policy, парсинг `/home`.
- OpenAPI 3.1 валидируется `openapi-spec-validator`.
- Запуск и визуальная сверка — на устройстве заказчика.

## Быстрый запуск без backend

На экране входа нажмите **«Продолжить без аккаунта (демо)»**.
В этом режиме приложение работает на `MockVkxRepository`: мок-каталог,
локальные OGG-сэмплы, без сети и токенов. Это же режим можно выбрать, если
backend ещё не поднят.

## Mock API

```bash
cd api/mock
npm install
npm start
```

Prism слушает `http://localhost:4010`; debug-сборка использует
`http://10.0.2.2:4010` (Android emulator).
