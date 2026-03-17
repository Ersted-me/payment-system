# individuals-api

Реактивный микросервис аутентификации и регистрации пользователей. Координирует создание учётных записей в Keycloak и профилей в Person Service.

OpenAPI-спецификация: [`openapi/individuals-api.yaml`](./openapi/individuals-api.yaml)

## Технологии

- **Spring Boot 4.0** (WebFlux — реактивный стек)
- **Spring OAuth2 Resource Server** — валидация JWT
- **Keycloak** — эмитент токенов, управление пользователями
- **Person Service SDK** — клиент для хранения профилей
- **Resilience4j** — Circuit Breaker, Bulkhead
- **OpenTelemetry** + Micrometer Prometheus — трассировка и метрики
- **MapStruct** — маппинг DTO
- **OpenAPI Generator** — генерация контроллерных интерфейсов из спецификации

## Требования

- Java 25+
- Docker (для запуска зависимостей и интеграционных тестов)
- Gradle 8+ (или используй обёртку `./gradlew`)

## Быстрый старт

**1. Запустить полный стек из корня репозитория**

```bash
docker-compose up
```

**2. Или запустить только зависимости и поднять сервис локально**

```bash
docker-compose up keycloak person-service-api -d
cd individuals-api
./gradlew bootRun
```

**3. Запустить тесты**

```bash
./gradlew test
```

| Сервис     | URL                                        |
|------------|--------------------------------------------|
| API        | http://localhost:8081/v1                   |
| Swagger UI | http://localhost:8081/swagger-ui.html      |
| Keycloak   | http://localhost:8080                      |
| Grafana    | http://localhost:3000                      |
| Prometheus | http://localhost:9090                      |

## Переменные окружения

| Переменная                    | Обязательная | Описание                                                               |
|-------------------------------|:---:|------------------------------------------------------------------------|
| `KEYCLOAK_URL`                | — | URL Keycloak (по умолчанию `http://localhost:8080`)                    |
| `KEYCLOAK_REALM`              | — | Название realm (по умолчанию `payment-system`)                         |
| `KEYCLOAK_CLIENT_ID`          | — | OAuth2 client ID (по умолчанию `individuals-api`)                      |
| `KEYCLOAK_CLIENT_SECRET`      | ✓ | Секрет OAuth2-клиента                                                  |
| `PERSON_SERVICE_URL`          | — | Базовый URL Person Service (по умолчанию `http://localhost:8083`)      |
| `OTLP_TRACING_GRPC_ENDPOINT`  | — | Endpoint OTLP-экспортера трейсов (по умолчанию `http://localhost:4317`)|

## API

Базовый URL: `/v1/auth`.

| Метод  | Эндпоинт          | Аутентификация | Описание                        |
|--------|-------------------|:--------------:|---------------------------------|
| POST   | `/registration`   | —              | Регистрация пользователя        |
| POST   | `/login`          | —              | Аутентификация, выдача токенов  |
| POST   | `/refresh-token`  | —              | Обновление access-токена        |
| GET    | `/me`             | JWT            | Данные текущего пользователя    |

### Поток регистрации

```
Client ──POST /registration──►  individuals-api
                                    │
                          1. Создать профиль ──► Person Service (PENDING)
                          2. Создать пользователя ──► Keycloak
                          3. Активировать профиль ──► Person Service (ACTIVE)
                          4. Вернуть токены ──► Client

  При ошибке на шаге 2 или 3 — компенсация: профиль удаляется из Person Service
```

## Структура проекта

```
src/main/java/com/ersted/individualsapi/
├── controller/
│   └── AuthController.java             # Реализует сгенерированный интерфейс AuthApi
├── service/
│   ├── UserService.java                # Оркестрация регистрации и компенсации
│   └── TokenService.java               # Логин, refresh, декодирование JWT
├── client/
│   ├── KeycloakClient.java             # Keycloak Admin API (retry, error handling)
│   └── PersonServiceClient.java        # Вызовы Person Service через SDK (CB, bulkhead)
├── config/
│   ├── SecurityConfig.java             # OAuth2 Resource Server, публичные эндпоинты
│   ├── KeycloakClientConfig.java       # WebClient + провайдер admin-токена
│   └── Resilience4jConfig.java         # Circuit Breaker и Bulkhead
├── exception/
│   ├── GlobalExceptionHandler.java     # Централизованная обработка ошибок
│   ├── KeycloakErrorHandler.java
│   └── PersonServiceErrorHandler.java
├── annotation/ + aspect/
│   ├── Counted.java                    # Кастомная аннотация для метрик
│   └── CountedAspect.java              # AOP: счётчики success/error в MeterRegistry
└── mapper/ + dto/ + utils/
```

## Конфигурация

```yaml
# application.yaml — ключевые настройки
keycloak:
  url: ${KEYCLOAK_URL:http://localhost:8080}
  realm: ${KEYCLOAK_REALM:payment-system}
  client-id: ${KEYCLOAK_CLIENT_ID:individuals-api}
  client-secret: ${KEYCLOAK_CLIENT_SECRET}
  requests-retry:
    attempts: 3
    delay-seconds: 1
    request-timeout-seconds: 10

resilience4j:
  circuitbreaker:
    sliding-window-size: 10
    failure-rate-threshold: 50
    wait-duration-in-open-state-seconds: 10
  bulkhead:
    max-concurrent-calls: 20
```

## Наблюдаемость

| Инструмент | Назначение |
|-----------|-----------|
| `@Counted("individuals.api.*")` | Счётчики вызовов с тегами `status=success/error` |
| `@Observed` | Трейсы через Micrometer Observation API |
| `GET /actuator/prometheus` | Метрики в формате Prometheus |
| OTLP gRPC | Экспорт трейсов в Grafana Alloy |

## Тесты

```bash
./gradlew test
```

| Класс | Тип |
|-------|-----|
| `UserServiceTest` | Unit (Mockito) |
| `TokenServiceTest` | Unit (Mockito) |
| `KeycloakClientTest` | Unit (WireMock) |
| `AuthControllerTest` | Integration (Testcontainers Keycloak) |

Интеграционные тесты поднимают реальный Keycloak в Docker-контейнере через Testcontainers. Вызовы Person Service заглушаются через WireMock.
