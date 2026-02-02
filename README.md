# Payment System

Микросервисная платежная система с аутентификацией через Keycloak и полным стеком observability (метрики, логи, трейсы).

## Архитектура

```
payment-system/
├── individuals-api/        # REST API сервис (Spring Boot WebFlux)
├── keycloak/              # Конфигурация Keycloak (realm-export.json)
├── alloy/                 # Grafana Alloy (сбор телеметрии)
├── prometheus/            # Хранение метрик
├── loki/                  # Агрегация логов
├── tempo/                 # Хранение трейсов
├── grafana/               # Мониторинг и визуализация
├── docker-compose.yaml    # Оркестрация инфраструктуры
└── Makefile               # Автоматизация сборки и деплоя
```

## Технологический стек

### Backend
- **Java 24** + **Spring Boot 3.5.0**
- **Spring WebFlux** (реактивный, неблокирующий)
- **Spring Security** (OAuth2 Resource Server)
- **Gradle 8.14.3** (Kotlin DSL)

### Аутентификация
- **Keycloak 26.2** (OAuth2/OpenID Connect)
- JWT Bearer токены
- Password Grant + Refresh Token

### Observability
- **Prometheus** — метрики
- **Loki** — логи
- **Tempo** — трейсы
- **Grafana Alloy** — сбор и маршрутизация телеметрии
- **Grafana** — визуализация

### База данных
- **PostgreSQL 17** (для Keycloak)

## Требования

- Docker и Docker Compose
- Make (опционально, для удобных команд)
- Java 24 и Gradle 8.14.3 (для локальной разработки)

## Быстрый старт

### Запуск всего стека

```bash
make up
```

Команда соберет и запустит все сервисы в фоновом режиме.

### Только инфраструктура (без API)

```bash
make infra
```

### Доступ к сервисам

| Сервис | URL | Учетные данные |
|--------|-----|----------------|
| Individuals API | http://localhost:8081 | JWT аутентификация |
| Keycloak Admin | http://localhost:8080 | admin / admin |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |

## Makefile команды

```bash
make up              # Сборка и запуск всех сервисов
make start           # Запуск существующих контейнеров
make stop            # Остановка всех сервисов
make restart         # Перезапуск всех сервисов
make rebuild         # Пересборка без кеша
make logs            # Просмотр логов всех сервисов
make logs-individuals-api  # Логи конкретного сервиса
make ps              # Статус контейнеров
make clean           # Полная очистка (контейнеры, volumes)
```

## API Endpoints

**Base URL:** `http://localhost:8081/v1`

### Регистрация пользователя

```bash
POST /v1/auth/registration
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "confirm_password": "password123"
}
```

**Ответ (201 Created):**
```json
{
  "access_token": "eyJ...",
  "refresh_token": "eyJ...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

### Авторизация

```bash
POST /v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

### Обновление токена

```bash
POST /v1/auth/refresh-token
Content-Type: application/json

{
  "refresh_token": "eyJ..."
}
```

### Получение информации о пользователе

```bash
GET /v1/auth/me
Authorization: Bearer <access_token>
```

**Ответ (200 OK):**
```json
{
  "id": "user-uuid",
  "email": "user@example.com",
  "roles": ["default-roles-payment-system"],
  "created_at": "2024-01-15T10:30:00Z"
}
```

### Мониторинг

```bash
GET /actuator/health      # Health check
GET /actuator/prometheus  # Метрики для Prometheus
```

## Локальная разработка

### Сборка JAR

```bash
cd individuals-api
./gradlew clean bootJar
```

### Запуск локально

```bash
# Установка переменных окружения
export KEYCLOAK_URL=http://localhost:8080
export KEYCLOAK_REALM=payment-system
export KEYCLOAK_CLIENT_ID=individuals-api
export KEYCLOAK_CLIENT_SECRET=8wb8HtX44VRUfquzs08Fk1dDH74d9vKc
export OTLP_TRACING_ENDPOINT=http://localhost:4317

# Запуск
java -jar build/libs/individuals-api-0.0.1.jar
```

### Запуск тестов

```bash
cd individuals-api
./gradlew test
```

## Конфигурация

### Переменные окружения

| Переменная | Описание | Значение по умолчанию |
|------------|----------|----------------------|
| `KEYCLOAK_URL` | URL Keycloak сервера | `http://keycloak:8080` |
| `KEYCLOAK_REALM` | Название realm | `payment-system` |
| `KEYCLOAK_CLIENT_ID` | ID клиента | `individuals-api` |
| `KEYCLOAK_CLIENT_SECRET` | Секрет клиента | см. docker-compose |
| `OTLP_TRACING_ENDPOINT` | Endpoint для трейсов | `http://alloy:4317` |
| `OTLP_TRACING_TRANSPORT` | Транспорт (grpc/http) | `grpc` |

### Конфигурационные файлы

- `individuals-api/src/main/resources/application.yaml` — конфигурация приложения
- `prometheus/prometheus.yml` — настройки Prometheus
- `loki/config.yaml` — настройки Loki
- `tempo/config.yaml` — настройки Tempo
- `alloy/config.alloy` — настройки Grafana Alloy
- `keycloak/realm-export.json` — экспорт realm Keycloak

## Структура исходного кода

```
com.ersted/
├── IndividualsApiApplication.java    # Entry point
├── controller/
│   └── AuthController.java           # REST endpoints
├── service/
│   ├── UserService.java              # Бизнес-логика пользователей
│   └── TokenService.java             # Работа с токенами
├── client/
│   └── KeycloakClient.java           # HTTP клиент для Keycloak
├── provider/
│   └── KeycloakAdminTokenProvider.java  # Кеширование admin токена
├── config/
│   ├── KeycloakProperties.java       # Конфигурация Keycloak
│   ├── SecurityConfig.java           # Spring Security
│   └── WebConfig.java                # WebClient конфигурация
├── exception/
│   ├── ValidationException.java
│   ├── UnauthorizedException.java
│   └── handler/
│       └── GlobalExceptionHandler.java
└── dto/                              # Сгенерировано из OpenAPI
    ├── UserRegistrationRequest.java
    ├── TokenResponse.java
    └── ...
```

## OpenAPI спецификация

Полная спецификация API находится в файле:
```
individuals-api/openapi/individuals-api.yaml
```

DTO и интерфейсы контроллеров генерируются автоматически при сборке.

## Docker

### Сеть
Все сервисы находятся в сети `payment-system-network`.

### Volumes
- `keycloak_pg-data` — данные PostgreSQL для Keycloak
- `prometheus-data` — данные метрик
- `loki-data` — данные логов
- `tempo-data` — данные трейсов
- `grafana-data` — конфигурация Grafana

### Health Checks
- Keycloak ожидает готовности PostgreSQL
- Individuals API проверяется по `/actuator/health`
- Остальные сервисы проверяются по статусу запуска


## Мониторинг

### Grafana Dashboards

После запуска откройте Grafana по адресу http://localhost:3000 и настройте data sources:
- Prometheus: `http://prometheus:9090`
- Loki: `http://loki:3100`
- Tempo: `http://tempo:3200`

### Метки для автоматического обнаружения

Контейнеры с метками автоматически обнаруживаются Alloy:
```yaml
labels:
  monitoring.enabled: "true"
  monitoring.port: "8080"
  monitoring.path: "/actuator/prometheus"
```
