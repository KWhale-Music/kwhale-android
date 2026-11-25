# AGENTS.md — kwhale-android

<!-- gh-ship:start -->
## Git & GitHub Workflow (обязательно для всех агентов)

- **Репозиторий:** `KWhale-Music/kwhale-android` (`origin`).
- **Правило завершения задачи:** перед тем как считать задачу закрытой, прогнать проверки/тесты, проверить `git status`, сделать осмысленный `git commit` и `git push` в `origin`.
- **Секреты:** никогда не коммитить `.env`, `secrets.env`, ключи подписи (`*.jks`) и дампы БД (`*.dump`); на репозитории стоит `pre-push` hook.
- **Релизы/публикация:** все изменения фиксируются на GitHub; крупные бинарники (`.apk`, `.flatpak`) — только через GitHub Releases по тегу `v*`.
<!-- gh-ship:end -->
