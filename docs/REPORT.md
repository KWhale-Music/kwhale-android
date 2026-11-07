# RE-отчёт: оригинал (`ua.itaysonlab.orig`) 8.14.2_pub

> Имя и символы исходного приложения маскированы как `orig`/`Orig` — подлинные имена в архиве `re/`.

Реверс выполнен по артефакту `re/Orig_8.14.2_pub_100150.apk`.
Отчёт покрывает **ядро** (плеер, библиотека, поиск, плейлисты, альбом/артист, настройки/темы)
глубоко и **вырезанный объём** — обзорно, как зафиксировано в решениях.

## 0. Артефакт и метод

| | |
|---|---|
| Файл | `re/Orig_8.14.2_pub_100150.apk` (37 341 670 байт) |
| SHA-256 | `56c33336b347d7126227174b34d2cf92f6d9d43d0e70c78638903f305cb64b92` |
| Пакет | `ua.itaysonlab.orig` |
| Версия | `8.14.2_pub`, versionCode `100150` |
| SDK | min 23 / target 36 / compile 37 |
| Подпись | self-signed (исходная) |

Инструменты: **REA** (`rea-agents@6.7.0`, jadx-headless-mcp 0.7.1) — манифест, инвентарь,
поиск/инспекция классов; **apktool 3.0.3** — декодирование ресурсов в `re/decoded/`.

> Дополнение: после первичного отчёта сделан полный JADX-дамп всех трёх DEX
> (20 527 `.java`, `re/decompiled/sources`), см. `re/README.md` и `tools/decompile.sh`.
> Native-слой разобран отдельно в `docs/NATIVE.md`.

Ограничения метода: R8-обфускация (20 540 классов, имена вида `defpackage.AbstractC0001z`),
UI строится кодом кастомного liquid-движка, а не декларативными XML-экранами. Полный jadx-дамп
не делался — анализ точечный, по читаемым пакетам `ua.itaysonlab.*`, `bruhcollective.*`,
`ru.utkacraft.*`.

## 1. Верхнеуровневая архитектура

- Application: `ua.itaysonlab.orig.OrigApplication`.
- Единственная activity: `ua.itaysonlab.orig.activity.AppActivity` (`singleTask`, splash-тема,
  `configChanges`, обработка `vk.com/audio*`, `/music*`, `/artist/*`).
- Сервисы: `...playback.service.PlaybackService` (Media3 `MediaLibraryService`),
  `...downloader.service.DownloaderService`, `...cache.v2.DedicatedCacheService`,
  `...service.integrations.LibOrigService`, `...service.AutostartLauncherService`.
- Провайдеры/приёмники: `OrigArtworkContentProvider`, `androidx.startup.InitializationProvider`,
  `MediaButtonReceiver`, `AutostartReceiver`.
- Мультиплатформенные точки: Glance-виджеты (`PlayerGlanceWidgetReceiver`,
  `SmallPlaylistsGlanceWidgetReceiver`), `ua.itaysonlab.origtv.OrigTvEntrypoint` (LEANBACK),
  `...playback.auto2.OrigCarAppService` (`androidx.car.app`), Cast
  (`OrigCastContextProvider`).
- Тема приложения: `@style/AppTheme.Orig.Liquid.DefaultDark` → `OrigAppThemeBase.MediaRouter`
  → `OrigAppThemeBase` (`Theme.Material3.Dark`).
- Liquid-движок: `ru.utkacraft.liquidnavigation.content.LFileProvider`, meta-data
  `liquid-themes-supported=true`, шаблонные layout-ы `box_* / row_* / column_* / complex_* /
  radio_*` как строительные блоки.

## 2. Главный каркас (`res/layout/activity_app.xml`)

```
FrameLayout base_frag (чёрный)
├── FrameLayout liquid_container      — сюда рендерятся экраны (background ?bg_page)
├── View dimmer                       — затемнение
├── CoordinatorLayout sheet_coord
│   └── FrameLayout bottom_sheet      — peekHeight 61dp, scrollable_bottom_sheet_behavior
│       └── ComposeView now_playing_compose_container
└── LinearLayout bottom_container (gravity bottom)
    └── NextDockView bottom_navigation — 72dp
```

Итог: контентный слой + now-playing-шторка (пик 61dp, Compose) + нижний док 72dp.

## 3. Навигация и док

- `ua.itaysonlab.origreborn.ui.nextdock.NextDockView` — кастомный View; методы
  `setOnItemClickListener`, `setOnLongItemClickListener`, `refreshTheme`, `setLegacyMode`.
- `res/layout/v6_nextdock_item.xml`: ячейка 72dp, `selected_frame` 46×46dp
  (`@drawable/rounded_card_dock`), иконка 24dp, дополнительная click-зона 120dp.
- Док в пересоздании повторяет геометрию и поведение (иконка + акцентная пилюля), табы:
  **Главная / Библиотека / Поиск / Настройки**.
- «Главная» в оригинале — каталог; в пересоздании — плейсхолдер (каталог вырезан по решению).

## 4. Now Playing

- В оригинале — `ComposeView` внутри bottom sheet с пиком 61dp.
- В пересоздании: **мини-плеер 61dp** над доком + **полноэкранный «Сейчас играет»**
  (обложка, seek, транспорт, лайк, shuffle/repeat), поверх мок-движка.

## 5. Дизайн-система

### 5.1 Токены темы (`res/values/attrs.xml`)

`bg_page`, `bg_primary`, `divider`, `nextdock_background`, `scrim_color`,
`scrim_color_lighter`, `text_primary`, `text_secondary`, `text_inverse`.

### 5.2 Базовые значения (тёмная по умолчанию)

| Токен | Тёмная | Светлая | AMOLED |
|---|---|---|---|
| windowBackground | `#121212` | `#FFFFFF` | `#000000` |
| bg_primary | `#202020` | `#F0F0F0` | `#121212` |
| nextdock_background | `#121212` | `#FFFFFF` | `#000000` |

Акцент по умолчанию: `theme_catalog.default_accent_id = 2` → `#71AAEB`
(в `colors.xml` базовый `accent_main = @color/sky_300`).

### 5.3 Типографика

- **VK Sans Display** (Regular/Medium/DemiBold/Bold + Condensed DemiBold) — заголовки.
- **VK Sans Text** (Light/Regular/Medium/DemiBold/Bold) — тело.
- Размеры из layout-ов: 32sp bold (имя артиста), 21sp bold (заголовок плейлиста),
  16sp (название трека), 14sp (вторичный текст), 13sp (сниппет/длительность).

### 5.4 Формы и размеры

| Элемент | Значение |
|---|---|
| Док | 72dp, выбранная пилюля 46dp, иконка 24dp |
| Трек-строка | обложка 48dp, padding 16dp, title 16sp, author 14sp, duration 13sp |
| Плейлист-карточка | 131dp (крупная — 220dp) |
| Шапка артиста | 260dp (`artist_header_v2c`: фото + scrim + имя 32sp + Play 42dp) |
| Now-playing peek | 61dp |
| Шапка плейлиста | обложка 170dp, title 21sp bold (`v7_playlist_header_type2`) |

## 6. Каталог тем и акцентов (`assets/theme_catalog.json`)

- **37 тем** в 4 группах: default (3), light (4), dark (14), community (16).
- **129 акцентов** в 12 группах: default, community, ios, android_q, xmb, solarized,
  vk_me, uwp, nord, gruvbox, wd, adwaita.
- `theme_attrs` задают цвета: `nextdock_background`, `bg_primary`, `@windowBackground`,
  `pref_card_bg`, `alert_background` и др.
- В пересоздании: режимы **Тёмная / Светлая / AMOLED / Системная (monet)** + палитра
  из 47 акцентов оригинала + динамический цвет Android 12+.

## 7. Данные и домен (по классам)

| Слой | Классы (доказательства) |
|---|---|
| Треки/альбомы/плейлисты | `ua.itaysonlab.vkapi2.objects.music.playlist.AudioPlaylist`, `AudioPlaylistPermissions`, `AudioPlaylistSnippetEntry` |
| Методы | `...methods.audio.playlist.AudioGetPlaylist`, `AudioAddAudiosToPlaylist` |
| Поиск | `...methods.execute.SearchInProfile$SearchResponse`, `$SearchPlaylistResponse` |
| Каталог | `ua.itaysonlab.catalogkit.objects.Catalog2*` |
| Кеш | `ua.itaysonlab.origreborn.cache.realm.CachedLibrary`, `CachedPlaylistLibrary` |

Пересоздание **зеркалит домен**: `Track`, `Album`, `Artist`, `Playlist`, `User`
(`core:model`), без привязки к сетевым DTO.

## 8. Ресурсы

- 564 layout-файла; большинство — generic-шаблоны liquid-движка.
- Реальные экранные/элементные: `activity_app`, `artist_header_v2c/v2cb`,
  `list_item_track`, `list_item_track_mainonly`, `list_item_track_swipe`,
  `list_item_track_collection`, `list_item_playlist*`, `v6_nextdock_item`,
  `v7_playlist_header_*`, `origui_toolbar_*`, `c3_*` (каталог), `podcast_*`.
- Шрифты: 10 начертаний VK Sans (9 ttf + 1 otf); в пересоздание перенесены 9 ttf.
- Assets: `theme_catalog.json`, `orig_eq_prebuilt*.json`, `orig_default_artwork.webp`,
  ML Kit barcode-модели (QR-сканер), `PublicSuffixDatabase`.

## 9. Карта экранов пересоздания

| Экран | Основание |
|---|---|
| Вход (заглушка) | решение: минимальный stub, без реальной авторизации |
| Главная | плейсхолдер (каталог вырезан) |
| Библиотека: Треки / Альбомы / Артисты / Плейлисты | `list_item_track*`, `list_item_playlist*`, токены |
| Поиск | `SearchInProfile.SearchResponse` (секции), `list_item_track` |
| Альбом | `list_item_track`, hero-шапка, токены |
| Артист | `artist_header_v2c` (фото 260dp, имя 32sp, Play 42dp) |
| Плейлист | `v7_playlist_header_type2` (обложка 170dp, title 21sp) |
| Настройки/темы | `theme_catalog.json`, `attrs.xml` |
| Сейчас играет | now-playing-контейнер + Compose |

## 10. Вырезано (обзор)

| Область | Доказательство в артефакте |
|---|---|
| Каталог/кураторы | `catalogkit.objects.Catalog2*`, layout-ы `c3_*` |
| Подкасты/книги | layout-ы `podcast_longread`, `podcast_simple_view`, `podcast_slider_item` |
| Виджеты | Glance: `PlayerGlanceWidgetReceiver`, `SmallPlaylistsGlanceWidgetReceiver` |
| Android Auto | `androidx.car.app`, `OrigCarAppService`, `@xml/automotive_app_desc` |
| Android TV | `ua.itaysonlab.origtv.OrigTvEntrypoint` (LEANBACK_LAUNCHER) |
| Cast | meta-data `com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME` |
| Эквалайзер | `assets/orig_eq_prebuilt.json`, `orig_eq_prebuilt_vk.json` |

По решению также вне объёма: радио/миксы/рекомендации, Last.fm, тексты песен, дизлайк,
офлайн-кеш. В этом проходе они не перепроверялись по классам — это отдельные задачи.

## 11. Как пересоздание использует отчёт

- Токены и размеры → `core:design` (тема M3, VK Sans, формы, акценты).
- Скелет `activity_app` → `Scaffold` + док 72dp + мини-плеер 61dp + маршрут «Сейчас играет».
- Домен `vkapi2` → модели `core:model`; мок-каталог → `core:data`.
- Каталог тем → режимы и палитра акцентов в настройках.
- Вырезанный объём → не реализуется; точка расширения — `OrigRepository`.

## 12. Ограничения

- Пиксельная сверка экранов не выполнялась (приложение не запускалось в этой фазе;
  проверка — на стороне заказчика).
- Экранные композиции восстанавливались по ресурсам/токенам/типовым паттернам, а не
  декомпиляцией каждой обфусцированной screen-функции.
- Подпись/сертификаты оригинала не переносились; сборка — debug-подпись.

## 13. Уточнения по ходу пересборки (Views/XML)

После первой итерации UI был переписан с Compose M3 на **Android Views + XML**, как в оригинале.

Подтверждено точечным реверсом:

- Док: `NextDockView.Signature(ArrayList)` инфлейтит `v6_nextdock_item` на каждую вкладку;
  `metrica(String, ...)` выбирает вкладку по ID.
- `AppActivity`: `OPEN_LIBRARY → "3"`, `OPEN_MENU → "4"`; по умолчанию `"0"`.
- `NextDockView.refreshTheme()`: выбранная вкладка — `R.attr.global_accent`,
  остальные — `R.attr.text_secondary`.
- Реальные иконки вкладок: `ic_home_outline_28`, `ic_search_outline_28`,
  `ic_music_note_wave_outline_28`, `ic_music_outline_28`, `ic_profile_28`
  → порядок: Главная / Поиск / Микс / Библиотека / Меню.
- Каркас: `activity_app.xml` (`liquid_container`, `dimmer`, `sheet_coord`/`bottom_sheet`
  peek 61dp, `now_playing_compose_container`, `bottom_container`/`NextDockView` 72dp).
- Каталог: блоки `c3_*` (баннер 155dp, кнопки 48dp, карточки 131dp, ссылки 102dp).
- Детали: `artist_header_v2c` (260dp, имя 32sp, Play 42dp), `v7_playlist_header_type2`
  (обложка 170dp, title 21sp), `v5_shuffler`, `list_item_track*`.

Ограничение: полный jadx-дамп в этой итерации не завершился (частичный), поэтому
часть композиций восстанавливалась по decoded-ресурсам и точечным вызовам REA.

## 14. Итоговый UI-редизайн

- Views/XML-каркас по `activity_app.xml`; Compose только для now-playing.
- Док: 5 вкладок (`ic_home_outline_28`, `ic_search_outline_28`,
  `ic_radiowaves_left_and_right_outline_28`, `ic_music_outline_28`, `ic_profile_28`),
  выбранная пилюля, `global_accent` / `text_secondary`.
- Библиотека: пилюли «Библиотека/Загружено», плейлисты 140dp, «Мои аудиозаписи»,
  сортировка, shuffle, трек-строки с `⋮` и explicit.
- Темы: Light/Dark/AMOLED + DynamicColors (monet); акценты — **129** overlay-стилей
  `OrigAccent_<id>` из `theme_catalog.json`.
- Плейлисты: создание, добавление из меню трека, удаление из своего плейлиста.
- Now-playing: Compose-экран с градиентом, обложкой, seek, shuffle/repeat, лайком.


