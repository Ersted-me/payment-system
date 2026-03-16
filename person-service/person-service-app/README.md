# person-service-app

Spring Boot приложение, реализующее REST API для управления профилями физических лиц.

## Технологии

- **Spring Boot** (WebMVC, Data JPA, OAuth2 Resource Server)
- **PostgreSQL** + Flyway
- **Hibernate Envers** — аудит изменений сущностей
- **OpenTelemetry** + Micrometer Prometheus — трассировка и метрики
- **MapStruct** — маппинг DTO ↔ сущности
- **OpenAPI Generator** — генерация контроллерных интерфейсов из спецификации

## Структура проекта

```
src/main/java/com/ersted/personservice/
├── controller/
│   └── IndividualsController.java      # REST-контроллер, реализует сгенерированный IndividualsApi
├── service/
│   └── IndividualService.java          # Бизнес-логика
├── entity/
│   ├── Individual.java                 # Профиль физлица
│   ├── User.java                       # Персональные данные пользователя
│   ├── Address.java                    # Адрес
│   ├── Country.java                    # Справочник стран
│   └── status/
│       ├── IndividualStatus.java       # PENDING | ACTIVE | ARCHIVED
│       └── CountryStatus.java          # ACTIVE | DISABLED
├── repository/
│   ├── IndividualRepository.java
│   └── CountryRepository.java
├── mapper/
│   └── IndividualMapper.java           # MapStruct: DTO ↔ Entity
├── exception/
│   ├── NotFoundException.java          # → 404
│   ├── ValidateException.java          # → 400
│   └── handler/
│       └── GlobalExceptionHandler.java
├── config/
│   ├── SecurityConfig.java             # OAuth2 Resource Server, JWT
│   └── ObservabilityConfig.java        # Регистрация ObservedAspect
└── annotation/ + aspect/
    ├── Counted.java                    # Кастомная аннотация для подсчёта метрик
    └── CountedAspect.java              # AOP: записывает счётчики success/error в MeterRegistry
```

## API

Спецификация: [`openapi/person-service-api.yaml`](../openapi/person-service-api.yaml)

Базовый URL: `/v1`. Все эндпоинты требуют `Authorization: Bearer <JWT>`.

### Жизненный цикл профиля

```
          create
            ↓
         PENDING  ──active──→  ACTIVE  ──archive──→  ARCHIVED

  purge — удаляет профиль из любого статуса
```

## База данных

Схема `person`. Миграции управляются Flyway.

```
countries (справочник, ~197 стран)
    └── id, name, alpha2, alpha3, status

addresses
    └── id, country_id → countries, address, zip_code, city, state

users
    └── id, address_id → addresses, email, first_name, last_name, secret_key, filled

individuals
    └── id, user_id → users (UNIQUE), passport_number, phone_number, status, verified_at, archived_at
```

Все сущности (кроме `Country`) аудируются Hibernate Envers. История хранится в таблицах `*_aud` схемы `person`.

### Миграции

| Файл | Содержимое |
|------|-----------|
| `V0__create-tables.sql` | Создание схемы и всех таблиц |
| `V1__init-countries.sql` | Заполнение справочника стран |
| `V2__init-audit.sql` | Таблицы аудита Envers |

## Конфигурация

Переменные окружения описаны в [README корневого модуля](../README.md#переменные-окружения).

```yaml
# application.yaml — ключевые настройки
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${OAUTH2_ISSUER_URI}
  flyway:
    default-schema: person
  jpa:
    hibernate:
      ddl-auto: validate
```

## Наблюдаемость

| Инструмент | Назначение |
|-----------|-----------|
| `@Counted("person.service.api.individual.*")` | Счётчики вызовов методов с тегами `status=success/error` |
| `@Observed(name = "individual.*")` | Трейсы через Micrometer Observation API |
| `GET /actuator/prometheus` | Метрики в формате Prometheus |
| OTLP gRPC | Экспорт трейсов в Grafana Alloy |

## Тесты

```bash
./gradlew :person-service-app:test
```

| Класс | Тип |
|-------|-----|
| `IndividualServiceTest` | Unit (Mockito) |
| `IndividualsControllerTest` | Integration (Testcontainers PostgreSQL) |

Интеграционные тесты поднимают PostgreSQL в Docker-контейнере через Testcontainers. Каждый тест откатывает изменения через `@Transactional`.
