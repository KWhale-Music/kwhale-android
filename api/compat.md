> Историческая таблица маппинга оригинального приложения на KWhale Core. В reduced v1 вошли не все
> Символы исходного приложения маскированы как `orig`/`Orig` (подлинные имена — в `re/`).
> перечисленные пути: radio, uploads обложек/аватаров, similar/related и CRUD
> миксов исключены. Актуальный набор — `openapi.yaml`.

# Совместимость с оригинальным приложением 8.14.2

Таблица маппинга вызовов оригинала на KWhale Core API. Нужна для проверки
функционального паритета и для backend-команды.

## VK API (`api.vk.com|vk.ru/method/*`)

| Оригинал | Наш API |
|---|---|
| `auth.getAuthCodeStatus`, `auth.setAuthCodeStatus` | `/auth/*` (наш flow) |
| `auth.getExchangeToken`, `auth.refreshTokens` | `/auth/refresh` |
| `auth.getMobileSession` | `/auth/login` |
| `auth.processAuthCode`, `auth.processAuthCodeMulti` | `/auth/login` (наш flow) |
| `auth.validateAccount`, `auth.validatePhone` | `/auth/register`, `/me` |
| `users.get` | `/me` (профили других пользователей — вне Core) |
| `audio.get` | `/library/tracks`, `/albums/{id}/tracks`, `/playlists/{id}` |
| `audio.getPlaylists` | `/playlists`, `/library/playlists` |
| `audio.getPlaylistById` | `/playlists/{id}` |
| `audio.search` | `/search?type=tracks` |
| `audio.searchArtists` | `/search?type=artists` |
| `audio.searchPlaylists` | `/search?type=playlists` |
| `audio.searchMain` | `/search?type=all` |
| `audio.add` | `PUT /tracks/{id}/like` |
| `audio.delete` | `DELETE /tracks/{id}/like` |
| `audio.addDislike` | `PUT /tracks/{id}/dislike` |
| `audio.removeDislike` | `DELETE /tracks/{id}/dislike` |
| `audio.add/removeFromPlaylist` | `POST/DELETE /playlists/{id}/tracks` |
| `audio.reorderInPlaylist` | `PUT /playlists/{id}/tracks/order` |
| `audio.editPlaylist`, `audio.deletePlaylist` | `PATCH/DELETE /playlists/{id}` |
| `audio.set/deletePlaylistCoverPhoto` | `PUT/DELETE /playlists/{id}/cover` |
| `photos.saveAudioPlaylistCover` | `POST /uploads` + `PUT /playlists/{id}/cover` |
| `audio.getRelatedArtistsById` | `GET /artists/{id}/related` |
| `audio.getAudioIdsBySource` | `GET /mixes/{id}/tracks`, `GET /radio/stations/{id}/tracks` |
| `audio.getStreamMixSettings` | `GET/PUT /mixes/{id}/settings` |
| `audio.getAudioPreviewUrl` | `GET /tracks/{id}/preview` |
| `audio.followRadioStation` | `PUT /radio/stations/{id}/follow` |
| `audio.unfollowRadioStation` | `DELETE /radio/stations/{id}/follow` |
| `audio.recommendationsOnboarding` | `GET/PUT /recommendations/onboarding` |
| `stats.trackEvents` | `POST /events` |
| `storage.get/set` | `/config` + локальные настройки |
| `apps.get` + `OrigNative.x02` | не используется: наш `/auth/login` |
| `execute` + JS `API.audio.search*` | прямые `/search` эндпоинты |
| `execute` + JS `API.account.getPrivacySettings` | вне Core scope |
| `utils.resolveScreenName` | `GET /search?q=` или `/resolve` (future) |
| `audioBooks.*`, `ecosystem.*` | вне Core scope |

## UMA / Moosic (`api.moosic.io`)

| Оригинал | Наш API |
|---|---|
| `GET /user/info` | `GET /me` |
| `POST /oauth/vkconnect/vk/token` | `POST /auth/login` |
| `POST /oauth/token/` | `POST /auth/refresh` |
| `PUT/DELETE /track/{id}/like|dislike` | `PUT/DELETE /tracks/{id}/like|dislike` |
| `POST /track/stat`, `/track/async_stat` | `POST /events` |
| `GetPersonalRadio` | `GET /mixes?kind=personal` |
| `GetTopArtists` | `GET /recommendations/top-artists` |
| `GetTopTracks` | `GET /recommendations/top-tracks` |
| `GetRadioTags` | `GET /radio/tags` |
| `GetRadioArtists` | `GET /radio/artists` |
| `GetUserInfo$Preapi` | `GET /me` |

## Внешние источники

| Оригинал | Наш API |
|---|---|
| Genius / LRCLIB / Musixmatch / VK lyrics | `GET /tracks/{id}/lyrics` |
| iTunes / MusicBrainz metadata | серверная сторона |
| Last.fm scrobbling | вне Core scope |
| OTA `orig.app` | вне Core scope |
| Firebase/AppMetrica | не используем; при необходимости `POST /events` |

## Правила

- Клиент не знает VK-методов и не формирует `method`-запросы.
- Все сущности — наши DTO (`Track`, `Album`, `Artist`, `Playlist`, `Mix`,
  `RadioStation`, `Lyrics`), а не VK JSON.
- Если для паритета нужен новый блок главной, добавляем `HomeSection.kind`
  и `HomeItem`-тип; это эволюционное изменение `/v1`.
