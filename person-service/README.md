# person-service

Микросервис управления профилями физических лиц. Хранит персональные данные пользователей, их адреса и статусы жизненного цикла.

## Модули

| Модуль | Описание |
|--------|----------|
| [`person-service-app`](./person-service-app/README.md) | Spring Boot приложение — REST API и бизнес-логика |
| [`person-service-sdk`](./person-service-sdk/README.md) | Java-библиотека для вызова API из других сервисов |

OpenAPI-спецификация: [`openapi/person-service-api.yaml`](./openapi/person-service-api.yaml)

## Требования

- Java 25+
- Docker (для запуска зависимостей и интеграционных тестов)
- Gradle 9+ (или используй обёртку `./gradlew`)

## Быстрый старт

**1. Запустить инфраструктуру**

```bash
docker-compose up postgres -d
```

**2. Собрать и опубликовать SDK локально**

```bash
./gradlew :person-service-sdk:publishToMavenLocal
```

**3. Запустить приложение**

```bash
./gradlew :person-service-app:bootRun
```

**4. Запустить тесты**

```bash
./gradlew :person-service-app:test
```

## Переменные окружения

| Переменная | Обязательная | Описание                                                                |
|-----------|:---:|-------------------------------------------------------------------------|
| `DATABASE_URL` | ✓ | JDBC URL, например `jdbc:postgresql://localhost:5432/person`            |
| `DATABASE_USERNAME` | ✓ | Пользователь БД                                                         |
| `DATABASE_PASSWORD` | ✓ | Пароль БД                                                               |
| `OAUTH2_ISSUER_URI` | ✓ | URI эмитента JWT, например `http://keycloak:8080/realms/payment-system` |
| `OTLP_TRACING_GRPC_ENDPOINT` | — | Endpoint OTLP-экспортера трейсов (по умолчанию `http://localhost:4317`) |
