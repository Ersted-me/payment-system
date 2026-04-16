# wallet-service

Микросервис управления кошельками и транзакциями. Реализует операции пополнения (DEPOSIT), вывода средств (WITHDRAWAL) и переводов (TRANSFER) с асинхронной финализацией через Kafka.

OpenAPI-спецификация: [`openapi/wallet-service-api.yaml`](./openapi/wallet-service-api.yaml)

## Технологии

- **Spring Boot 4.0** (Web MVC)
- **Spring OAuth2 Resource Server** — валидация JWT (Keycloak)
- **Spring Data JPA** + **Flyway** — хранение данных и миграции схемы
- **Spring Kafka** + **Apache Avro** + **Schema Registry** — асинхронные события
- **Transactional Outbox** + **Idempotent Consumer** — надёжная доставка сообщений
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
docker-compose up keycloak kafka schema-registry postgres-wallet -d
cd wallet-service
./gradlew bootRun
```

**3. Запустить тесты**

```bash
./gradlew test
```

| Сервис        | URL                                         |
|---------------|---------------------------------------------|
| API           | http://localhost:8084/v1                    |
| Swagger UI    | http://localhost:8084/swagger-ui/index.html |
| Keycloak      | http://localhost:8080                       |

## Переменные окружения

| Переменная                                  | Обязательная | Описание                                             |
|---------------------------------------------|:---:|------------------------------------------------------|
| `DATABASE_URL`                              | ✓ | JDBC URL базы данных (`jdbc:postgresql://...`)        |
| `DATABASE_USERNAME`                         | ✓ | Имя пользователя БД                                  |
| `DATABASE_PASSWORD`                         | ✓ | Пароль БД                                            |
| `OAUTH2_ISSUER_URI`                         | ✓ | URI эмитента JWT (Keycloak realm endpoint)           |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS`            | ✓ | Kafka bootstrap servers (`host:port`)                |
| `SPRING_KAFKA_PROPERTIES_SCHEMA_REGISTRY_URL` | ✓ | URL Schema Registry                                |
| `FEE_DEPOSIT`                               | — | Комиссия за депозит (по умолчанию `0.0`)             |
| `FEE_WITHDRAWAL`                            | — | Комиссия за вывод (по умолчанию `0.015`)             |
| `FEE_TRANSFER`                              | — | Комиссия за перевод (по умолчанию `0.01`)            |
| `OUTBOX_SCHEDULER_FIXED_DELAY_MS`           | — | Интервал опроса Outbox в мс (по умолчанию `5000`)    |

## API

Базовый URL: `/v1`. Все эндпоинты требуют Bearer JWT.

### Кошельки

| Метод | Эндпоинт                        | Описание                              |
|-------|---------------------------------|---------------------------------------|
| POST  | `/wallets`                      | Создание кошелька                     |
| GET   | `/wallets/{walletUuid}`         | Получение информации о кошельке       |
| GET   | `/wallets/user/{userUuid}`      | Все кошельки пользователя             |

### Транзакции

| Метод | Эндпоинт                            | Описание                                           |
|-------|-------------------------------------|----------------------------------------------------|
| POST  | `/transactions/{type}/init`         | Расчёт комиссии без создания транзакции            |
| POST  | `/transactions/{type}/confirm`      | Создание и запуск транзакции (`type`: DEPOSIT / WITHDRAWAL / TRANSFER) |
| GET   | `/transactions/{transactionUuid}/status` | Статус транзакции                            |
| GET   | `/transactions`                     | Поиск с фильтрами и пагинацией                     |

### Жизненный цикл транзакции

```
Client ──POST /transactions/DEPOSIT/confirm──► wallet-service
                                                   │
                                       1. Сохранить транзакцию (PENDING)
                                       2. Записать DepositRequestedEvent в OUTBOX
                                       3. Вернуть ответ клиенту
                                                   │
                              OutboxScheduler (каждые 5 сек)
                                       4. Прочитать PENDING из OUTBOX
                                       5. Отправить в Kafka ──► payment-gateway
                                                   │
                              Kafka (DepositCompletedEvent)
                                       6. Получить результат от payment-gateway
                                       7. Дедупликация (InboxService)
                                       8. Зачислить средства / пометить FAILED
```

## Структура проекта

```
src/main/java/com/ersted/walletservice/
├── controller/
│   ├── WalletController.java           # Реализует сгенерированный интерфейс WalletsApi
│   └── TransactionController.java      # Реализует сгенерированный интерфейс TransactionsApi
├── service/
│   ├── WalletService.java              # Создание и поиск кошельков
│   ├── TransactionService.java         # Оркестрация через Strategy, поиск, завершение
│   ├── OutboxService.java              # Сохранение событий в Outbox
│   └── InboxService.java               # Дедупликация входящих событий
├── service/strategy/
│   ├── TransactionStrategy.java        # Интерфейс: getType(), init(), confirm()
│   ├── CompletableStrategy.java        # Интерфейс: complete()
│   ├── DepositStrategy.java            # Депозит: расчёт комиссии + Outbox
│   ├── WithdrawalStrategy.java         # Вывод: проверка баланса + Outbox
│   └── TransferStrategy.java           # Перевод: списание и зачисление
├── kafka/
│   ├── DepositEventConsumer.java       # KafkaListener: DepositCompletedEvent
│   └── WithdrawalEventConsumer.java    # KafkaListener: WithdrawalCompletedEvent
├── scheduler/
│   └── OutboxScheduler.java            # @Scheduled: отправка OUTBOX → Kafka (retry max=3)
├── kafka/avro/
│   ├── AvroSchemaRegistrySerializer.java
│   └── AvroSchemaRegistryDeserializer.java
├── config/
│   ├── SecurityConfig.java             # JWT-фильтр, permitAll для health и Swagger
│   ├── KafkaConfig.java                # Топики, ObjectMapper, Avro mixin
│   └── FeeProperties.java              # @ConfigurationProperties: fee.*
├── entity/ mapper/ repository/
└── exception/
    ├── ApiException.java
    └── GlobalExceptionHandler.java
```

## База данных

Миграции Flyway, схема `wallet`:

| Таблица        | Назначение                                                   |
|----------------|--------------------------------------------------------------|
| `WALLET_TYPES` | Справочник типов кошельков (валюта, статус)                  |
| `WALLETS`      | Кошельки пользователей (баланс, статус, тип)                 |
| `TRANSACTIONS` | История транзакций (тип, сумма, комиссия, статус, wallet)    |
| `OUTBOX`       | Исходящие события (Transactional Outbox pattern)             |
| `INBOX`        | Обработанные входящие события (Idempotent Consumer pattern)  |

## Тесты

```bash
./gradlew test
```

| Класс | Тип | Что покрыто |
|-------|-----|-------------|
| `WalletServiceTest` | Unit (Mockito) | Создание, поиск, ошибки типа кошелька |
| `TransactionServiceTest` | Unit (Mockito) | Init/confirm/complete/status, ошибки |
| `WalletControllerTest` | Integration (Testcontainers) | REST: CRUD кошельков с JWT |
| `TransactionControllerTest` | Integration (Testcontainers) | REST: все типы транзакций с JWT |
| `SecurityConfigTest` | Integration (Testcontainers) | 401 без токена, permitAll эндпоинты |
| `DepositEventConsumerTest` | Integration (Testcontainers + Kafka) | Зачисление, FAILED, дедупликация |
| `WithdrawalEventConsumerTest` | Integration (Testcontainers + Kafka) | Списание, FAILED, дедупликация |
| `OutboxSchedulerTest` | Integration (Testcontainers + Kafka) | Отправка PENDING, retry, FAILED |

Интеграционные тесты поднимают PostgreSQL и Kafka в Docker-контейнерах через Testcontainers.

## Диаграммы

Компонентная диаграмма (C4): [`architecture/wallet-service/Component.puml`](../architecture/wallet-service/Component.puml)
