# KWhale Core API — контракт

Системная спецификация KWhale v1: [`../../kwhale-v3/docs/KWHALE_V1_SPEC.md`](../../kwhale-v3/docs/KWHALE_V1_SPEC.md).

`openapi.yaml` — единственный источник истины для клиента и KWhale-v3.
Версия: **0.2.0** (Core music, 41 path).

## 1. Один origin

Клиент обращается **только** к `servers[0].url` (в debug — к mock).
Любой другой host — ошибка клиента.

- обложки/аватары — `ImageRef`, клиент грузит `GET /images/{id}`;
- аудио — `GET /tracks/{id}/stream`, без 302 на внешний CDN;
- скачивание — `GET /tracks/{id}/download`, тот же origin, всегда байты файла;
- тексты — `GET /tracks/{id}/lyrics`;
- загрузка своего аудио — `POST /uploads/tracks`;
- рекомендации, поиск, события — только через этот API;
- в JSON **нет** абсолютных `http(s)://` URL. Только `ImageRef` и пути.

## 2. Авторизация

- Регистрация только по инвайт-токену: `POST /auth/register` с `inviteToken`.
- `POST /auth/login` — email/password.
- `POST /auth/refresh` — refresh token, ротация.
- `POST /auth/logout` — отзыв refresh-токена.
- Дальше: `Authorization: Bearer <accessToken>`.

Токены:

- access — JWT, 15 минут;
- refresh — opaque, 30 дней, хранится на клиенте в encrypted storage;
- при 401 клиент один раз делает refresh и повторяет запрос.

## 3. Пагинация

`?cursor=<opaque>&limit=<1..100>`.

Ответы-страницы: `{ "items": [...], "nextCursor": "..." }`.
`nextCursor` отсутствует на последней странице.

## 4. Ошибки

RFC 7807, `application/problem+json`:

```json
{
  "type": "https://api.kwhale.example/problems/not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Track not found",
  "code": "not_found",
  "instance": "/v1/tracks/123"
}
```

## 5. Идемпотентность

`Idempotency-Key: <uuid>` поддерживается для:

- `POST /playlists`;
- `POST /playlists/{id}/tracks`;
- `PUT /albums/{id}/tracks/like`;
- `PUT /playlists/{id}/tracks/like`.

## 6. Медиа

### Аудио

- `GET /tracks/{id}/stream` — `audio/mpeg|mp4|ogg|flac`, `Accept-Ranges: bytes`;
- `Range` → `206 Partial Content`;
- без redirect на внешний host;
- `Cache-Control: private, max-age=0`.

### Скачивание

- `GET /tracks/{id}/download` — `Content-Disposition: attachment`;
- тот же host, те же права; всегда отдаёт байты (локальный файл или proxy remote).

### Изображения

- `ImageRef { id, width?, height?, blurHash? }`;
- `GET /images/{id}?size=xs|sm|md|lg|original`;
- `Cache-Control: public, max-age=31536000, immutable` для контентных id;
- поддержка `ETag`/`304`;
- при отсутствии обложки — placeholder.

## 7. Рекомендации и заглушки

- `GET /home` — секции из библиотеки; рекомендательные секции пустые.
- `GET /recommendations/top-artists`, `/genres`, `/onboarding` — простые данные/заглушки.
- `GET /mixes`, `GET /mixes/{id}/tracks` — заглушки до recommendation-модуля.
- Радио в контракт не входит.
- `POST /events` — raw-события для будущего рекомендательного слоя:
  `playbackId`, `listenedMs`, `trackDurationMs`, `startedAt`, `endedAt`,
  `timezoneOffsetMinutes`, `surface`, `sessionId`, `deviceId`, `query`,
  `resultCount`, `clickedPosition`.
- Лайк/дизлайк меняет библиотеку через `PUT/DELETE /tracks/{id}/like|dislike`;
  в `/events` это идёт как сигнал.

## 8. Версионирование

- База: `/v1`;
- `GET /config` возвращает `apiVersion`, `minClientVersion`, фича-флаги;
- ломающие изменения — `/v2`;
- добавление необязательных полей — в пределах `/v1`.

## 9. Mock и contract-тесты

- `api/mock/` — Prism для локального запуска;
- `core:network` unit-тесты используют OkHttp MockWebServer;
- mock не является продакшн-сервером;
- контрактные тесты проверяют, что клиент не выходит за пределы `openapi.yaml`.

## 10. Ограничения

- Тексты и метаданные лицензируются на стороне backend-команды;
- клиент не обращается к Genius/LRCLIB/Musixmatch/Last.fm/iTunes напрямую;
- рекомендации — отдельная команда/модуль; KWhale только принимает события.
