# payment-system

Микросервисная платёжная система с аутентификацией через Keycloak и полным стеком наблюдаемости (метрики, логи, трейсы).

## Сервисы

| Сервис | Описание | OpenAPI |
|--------|----------|---------|
| [`individuals-api`](./individuals-api/README.md) | Реактивный REST API — регистрация, аутентификация, управление токенами | [`individuals-api.yaml`](./individuals-api/openapi/individuals-api.yaml) |
| [`person-service`](./person-service/README.md) | Хранение и управление профилями физических лиц | [`person-service-api.yaml`](./person-service/openapi/person-service-api.yaml) |

## Архитектура

```
                          ┌─────────────────┐
                          │   individuals   │
              ┌───────────│      api        │◄──────────┐
              │           │   :8081         │           │
              │           └────────┬────────┘           │
       individuals-api             │                  Client
              │           ┌────────▼────────┐
              │           │    Keycloak     │
              │           │    :8080        │
              │           └─────────────────┘
              │
              │           ┌─────────────────┐
              └──────────►│  person-service │
                          │    :8083        │
                          └────────┬────────┘
                                   │
                          ┌────────▼────────┐
                          │   PostgreSQL    │
                          │    :5434        │
                          └─────────────────┘
```

Стек наблюдаемости: все сервисы отправляют трейсы (OTLP gRPC) и метрики (Prometheus) в **Grafana Alloy**, который маршрутизирует их в Tempo, Prometheus и Loki. Grafana предоставляет единый интерфейс визуализации.

## Требования

- Docker и Docker Compose
- Make (опционально, для удобных команд)
- Java 25 и Gradle 8+ (для локальной разработки)

## Быстрый старт

**Запустить весь стек**

```bash
make up
```

**Только инфраструктура (без API-сервисов)**

```bash
make infra
```

**Только наблюдаемость (Alloy, Prometheus, Loki, Tempo, Grafana)**

```bash
make observability
```

## Адреса сервисов

| Сервис | URL | Учётные данные |
|--------|-----|----------------|
| Individuals API | http://localhost:8081 | JWT |
| Person Service | http://localhost:8083 | JWT |
| Keycloak Admin | http://localhost:8080 | admin / admin |
| Nexus | http://localhost:8082 | admin / admin |
| Grafana | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | — |
| Loki | http://localhost:3100 | — |
| Tempo | http://localhost:3200 | — |
| Grafana Alloy | http://localhost:12345 | — |

## Makefile

```bash
make up           # Сборка и запуск всех сервисов
make start        # Запуск существующих контейнеров
make stop         # Остановка всех сервисов
make restart      # Остановка + сборка + запуск
make rebuild      # Пересборка без кеша
make logs         # Логи всех сервисов
make logs-<name>  # Логи конкретного сервиса, например make logs-individuals-api
make ps           # Статус контейнеров
make clean        # Полная очистка (контейнеры, volumes)
make infra        # Только инфраструктура (Keycloak + наблюдаемость)
make observability # Только стек наблюдаемости
```

## Инфраструктура

Конфигурационные файлы лежат в `infrastructure/`:

| Директория | Содержимое |
|------------|-----------|
| `infrastructure/keycloak/` | `realm-export.json` — realm `payment-system` с преднастроенными клиентами |
| `infrastructure/alloy/` | `config.alloy` — сбор OTLP-трейсов и проксирование в Tempo / Prometheus / Loki |
| `infrastructure/prometheus/` | `prometheus.yml` — scrape-конфигурация |
| `infrastructure/loki/` | `config.yaml` — конфигурация хранилища логов |
| `infrastructure/tempo/` | `config.yaml` — конфигурация хранилища трейсов |
| `infrastructure/grafana/` | `provisioning/` — автоматическое подключение data sources |

Сервисы с меткой `monitoring.enabled=true` автоматически обнаруживаются Alloy через Docker Socket Proxy.

## Postman

Коллекция запросов: [`postman/individuals-api/Individuals-api.postman_collection.json`](./postman/individuals-api/Individuals-api.postman_collection.json)

## Диаграммы

Sequence-диаграммы и C4-модель находятся в [`architecture/`](./architecture/).
