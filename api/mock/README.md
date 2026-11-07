# Mock API

Не продакшн-сервер. Нужен, чтобы приложение и contract-тесты работали до
появления реального backend.

## Prism (локально)

```bash
cd api/mock
npm install
npm start
```

По умолчанию: `http://localhost:4010`.

Prism игнорирует базовый путь из `servers` (`/v1`) и мапит пути от корня.
Поэтому debug-сборка использует `API_BASE_URL=http://10.0.2.2:4010`
(без `/v1`), а production — `https://api.kwhale.example/v1`.

Из Android emulator host машины: `http://10.0.2.2:4010`.
Для физического устройства — `http://<ip-машины>:4010` и `adb reverse`.

## Docker

```bash
cd api/mock
docker compose up
```

## CI

В Gradle-тестах `core:network` используется OkHttp MockWebServer: он
поднимает HTTP-заглушку в JVM-тесте и проверяет Authorization, refresh и
path/query. Это эквивалент WireMock для нашего объёма.

## Важно

- Prism отвечает примерами из `openapi.yaml`; состояния (лайки, плейлисты)
  не сохраняются.
- Для сценариев с состоянием используйте WireMock-стабы или fake-repository.
- Mock не заменяет реальный API и не входит в релиз.
