> Reduced v1: радио удалено из контракта; миксы/рекомендации — заглушки до отдельного recommendation-модуля. Таблица ниже — историческая карта паритета VK X.

# Паритет: VK X 8.14.2 vs vkx-recreate

## Границы текущего этапа

Целевой объём — **Core music**, без:

- эквалайзера/DSP (`eq3_*`, `vkx_eq_prebuilt*.json`);
- Android Auto (`VkxCarAppService`, `as2_*`);
- Android TV (`ua.itaysonlab.vkxtv`);
- Cast (`VkxCastContextProvider`, `cast_*`);
- подкастов и аудиокниг (`podcast_*`, `AudioBook`);
- Spark/платежей/подписок;
- социального графа, профилей друзей и ленты новостей.

Разрешён один виджет — **плеер** (Glance, 4×2). Всё остальное — вне scope.

## Матрица

Статусы: `есть` — реализовано в прототипе; `частично` — есть каркас/мок;
`нет` — отсутствует; `вне scope` — сознательно исключено.

| Область | Оригинал (evidence) | Прототип | Статус | Контракт |
|---|---|---|---|---|
| Вход | `auth2_*`, `C4551z`, `C0442z`, `C8094z` | `AuthActivity` + `VkxApi` email/password, JWT | частично | `/auth/*` |
| Профиль | `VKProfile`, `MenuView` | заглушка «Вы» | частично | `/me` |
| Главная | `c3_*`, `HomeView` | `RemoteHomeView` читает `/home` | частично | `/home` |
| Поиск | `SearchInProfile`, `SearchView` | `RemoteSearchView` читает `/search` | частично | `/search` |
| Библиотека | `CachedLibrary`, `LibraryView` | `RemoteLibraryView` читает `/library/*` | частично | `/library/*` |
| Плейлисты | `AudioPlaylist`, `AudioPlaylistPermissions` | создание/добавление/удаление | частично | `/playlists/*` |
| Альбом | `AudioAlbum`, `AlbumView` | `RemoteCollectionView`, play/shuffle | частично | `/albums/{id}` |
| Артист | `ArtistInfo`, `ArtistView` | `RemoteCollectionView`, топ-треки и альбомы | частично | `/artists/{id}` |
| Трек | `AudioTrack` | API DTO + mapper в UI-модель | частично | `/tracks/{id}` |
| Воспроизведение | `PlaybackService`, `C4551z` | `PlaybackService` + MediaController + remote `streamUrl` | частично | `/tracks/{id}/stream` |
| Очередь | `now_playing_*`, `PlaybackService` | очередь в сервисе, shuffle/repeat | частично | клиент + `/events` |
| Скачивание/офлайн | `DownloaderService`, `cache_*`, SAF | WorkManager + локальный URI + секция «Скачано» | частично | `/tracks/{id}/download` |
| Тексты | `lyrics_*`, Genius/LRCLIB/Musixmatch/VK | `LyricsSheet` + `/tracks/{id}/lyrics` | частично | `/tracks/{id}/lyrics` |
| Радио/миксы | UMA/Moosic, `radio_*`, `mixes_v2_*` | `RemoteRadioView`: личные миксы и станции | частично | `/mixes/*`, `/radio/*` |
| Рекомендации | UMA/Moosic, `rec_screen_*` | onboarding + top-artists + home | частично | `/recommendations/*` |
| События/сигналы | `stats.trackEvents` | `EventTracker`: play/skip/pause в `/events` | частично | `/events` |
| Темы | `theme_catalog.json` (37 тем) | 37 тем + 4 режима + 129 акцентов | есть | локальные assets |
| Акценты | `theme_catalog.json` (129) | 129 | есть | локальные assets |
| Настройки | `settings_*`, `pref_*` | только темы | частично | `/config` + локально |
| Уведомление плеера | `PlaybackService` | `MediaSessionService` + медиа-уведомление | частично | клиент |
| Виджет | Glance `PlayerGlanceWidgetReceiver` | один Glance-виджет плеера | частично | клиент |
| EQ/DSP | `eq3_*`, `vkx_eq_prebuilt*` | нет | вне scope | — |
| Android Auto | `VkxCarAppService`, `as2_*` | нет | вне scope | — |
| Android TV | `vkxtv` | нет | вне scope | — |
| Cast | `cast_*` | нет | вне scope | — |
| Подкасты/книги | `podcast_*`, `AudioBook` | нет | вне scope | — |
| Spark/платежи | `spark_*` | нет | вне scope | — |
| Соцсеть | `newsfeed_*`, `VKProfile` | нет | вне scope | — |
| Сеть/контракт | VK API / UMA / Moosic | `core:network`, OpenAPI 3.1, mock | есть | `api/openapi.yaml` |

## Что считается «Core parity» для приёмки

1. Вход/выход, токены, восстановление сессии.
2. Главная: banner/actions/shelf/list/mix/radio-секции.
3. Поиск: подсказки, история, треки/альбомы/артисты/плейлисты.
4. Библиотека: лайки, плейлисты, сортировка, импорт локальных данных.
5. Альбом/артист/плейлист: шапка, play/shuffle, треклист.
6. Плеер: сервис, уведомление, audio focus, очередь, shuffle/repeat, sleep timer.
7. Скачивание/офлайн: прогресс, отмена, локальное воспроизведение, удаление.
8. Тексты: plain + synced.
9. Радио/миксы: персональное радио, миксы по seed, feedback.
10. Рекомендации: onboarding, «Для вас», похожие.
11. Темы: 37 тем, 129 акцентов, now-playing оформление.
12. Один Glance-виджет плеера.

## Маппинг оригинальных вызовов

| Оригинал | Наш API |
|---|---|
| `auth.*` | `/auth/*` |
| `users.get` | `/me`, `/users/{id}` |
| `audio.get`, `audio.getPlaylists` | `/library/tracks`, `/playlists` |
| `audio.search`, `audio.searchArtists`, `audio.searchPlaylists`, `audio.searchMain` | `/search?type=` |
| `audio.getPlaylistById` | `/playlists/{id}` |
| `audio.editPlaylist`, `audio.deletePlaylist` | `PATCH/DELETE /playlists/{id}` |
| `audio.add/removeFromPlaylist`, `audio.reorderInPlaylist` | `/playlists/{id}/tracks`, `/order` |
| `audio.set/deletePlaylistCoverPhoto` | `/playlists/{id}/cover`, `/uploads` |
| `audio.getRelatedArtistsById` | `/artists/{id}/related` |
| `audio.getAudioIdsBySource`, `audio.getStreamMixSettings` | `/mixes/{id}/tracks`, `/mixes/{id}/settings` |
| `audio.followRadioStation` / `unfollow` | `/radio/stations/{id}/follow` |
| `audio.recommendationsOnboarding` | `/recommendations/onboarding` |
| `audio.getAudioPreviewUrl` | `/tracks/{id}/preview` |
| `stats.trackEvents` | `/events` |
| UMA `GetPersonalRadio` | `/mixes?kind=personal` |
| UMA `GetTopArtists`, `GetTopTracks` | `/recommendations/top-*` |
| UMA `GetRadioTags`, `GetRadioArtists` | `/radio/tags`, `/radio/artists` |

## Ограничения

- Оригинальный UI местами серверный (`liquid`/`c3_*`); наш контракт описывает
  типизированные секции, а не произвольный UI-JSON.
- Полный пиксельный паритет не является целью M0–M4; цель — функциональный
  Core parity и совпадение ключевых размеров/токенов.
- Все внешние источники (тексты, метаданные, рекомендации) находятся за нашим
  API; клиент не обращается к ним напрямую.
