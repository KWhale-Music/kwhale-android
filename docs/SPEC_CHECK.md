# Проверка спецификации

Дата: 2026-11-05. Спецификация: `api/openapi.yaml` 0.2.0 (reduced Core v1).

## Результат

- OpenAPI 3.1 валиден (`openapi-spec-validator`).
- **41 path**, **50 schemas**.
- Все пути, которые вызывает `KwhaleApi`, присутствуют в спецификации.
- `tools/validate-openapi.sh` — зелёный.
- Unit-тесты `core:network` — зелёные.
- `:app:assembleDebug` — зелёный.
- Сервер KWhale проверяется отдельно: набор `/v1` FastAPI совпадает с путями
  спецификации (41/41).

## Сознательно не подключено в UI

- `POST /uploads/tracks` — endpoint есть, UI загрузки своего трека пока нет.
- `PUT /albums/{id}/tracks/like` / `PUT /playlists/{id}/tracks/like` — endpoint
  есть; в UI кнопка «Лайкнуть всё» добавлена для альбома/плейлиста.
- `/recommendations/*`, `/mixes*` — заглушки до рекомендательного модуля.

## Проверка single-origin

- `HostPolicyInterceptor` блокирует любой host, кроме `ApiConfig.baseHost`.
- Coil `ImageLoader` использует тот же interceptor.
- `AuthHttpDataSourceFactory` проверяет host медиапотока.
- `DownloadWorker` качает через `ApiClient` (тот же origin).
- `EventTracker`, поиск, тексты, миксы — через `KwhaleApi`.

## Остаточные риски

- Prism не отдаёт реальные аудиобайты/тексты; проверяется на реальном backend.
- Устройства/эмулятора нет; виджет, уведомление и bottom sheet не проверены визуально.
- `PlayerActivity` оставлен в манифесте как неиспользуемый legacy-экран.
