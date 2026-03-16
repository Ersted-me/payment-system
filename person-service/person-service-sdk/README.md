# person-service-sdk

Java-библиотека для вызова `person-service` API из других микросервисов. Генерируется автоматически из OpenAPI-спецификации с использованием Spring HTTP Interface (реактивный клиент).

## Требования

Потребитель должен иметь в classpath:

- `spring-webflux` (для `WebClient`)
- `reactor-core`

## Подключение

### Gradle

```kotlin
implementation("com.ersted:person-service-sdk:1.0.0-SNAPSHOT")
```

Для локальной разработки — опубликуй в локальный Maven:

```bash
./gradlew :person-service-sdk:publishToMavenLocal
```

### Maven

```xml
<dependency>
    <groupId>com.ersted</groupId>
    <artifactId>person-service-sdk</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Использование

SDK предоставляет интерфейс `IndividualsApi` — Spring HTTP Interface с реактивными методами (`Mono`). Все методы соответствуют эндпоинтам из [`openapi/person-service-api.yaml`](../openapi/person-service-api.yaml) и возвращают `Mono<ResponseEntity<T>>`.

### Конфигурация клиента (Spring Boot 4)

> **Важно.** В Spring Boot 4 `HttpServiceProxyFactory` вызывает баг: бин `IndividualsApi` не регистрируется в контексте Spring, из-за чего инъекция зависимости не работает. Используй вместо него `@ImportHttpServices` + `WebClientHttpServiceGroupConfigurer` — это официальный API Spring Boot 4 для HTTP-клиентов.

**1. Конфигурационный класс:**

```java
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(
        group = "person-service",
        basePackages = "com.ersted.personservice.sdk.api",
        types = { IndividualsApi.class },
        clientType = HttpServiceGroup.ClientType.WEB_CLIENT
)
public class SdkConfig {

    @Bean
    WebClientHttpServiceGroupConfigurer personServiceGroupConfigurer(Environment env) {
        String baseUrl = env.getProperty(
                "spring.http.serviceclient.person-service.base-url",
                "http://localhost:8080"
        );
        return groups -> groups
                .filterByName("person-service")
                .forEachClient((group, builder) -> builder.baseUrl(baseUrl));
    }

}
```

**2. Свойство в `application.yaml`:**

```yaml
spring:
  http:
    serviceclient:
      person-service:
        base-url: ${PERSON_SERVICE_URL:http://localhost:8081}/v1
```

После этого `IndividualsApi` доступен как обычный Spring-бин через `@Autowired` / constructor injection.

### Примеры

```java
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final IndividualsApi individualsApi;

    // Создание профиля
    public Mono<IndividualInfoResponse> registerProfile(IndividualCreateProfileRequest request) {
        return individualsApi.createIndividual(Mono.just(request))
                .map(ResponseEntity::getBody);
    }

    // Активация после подтверждения в Keycloak
    public Mono<Void> activateProfile(UUID individualId) {
        return individualsApi.activateIndividual(individualId)
                .then();
    }

    // Откат при ошибке регистрации
    public Mono<Void> rollback(UUID individualId) {
        return individualsApi.purgeIndividual(individualId)
                .then();
    }
}
```

## Модели

Все модели находятся в пакете `com.ersted.personservice.sdk.model`, сгенерированы из OpenAPI-спецификации.

> `alpha3` в адресе — код ISO 3166-1 alpha-3 (например `RUS`). Должен соответствовать одному из ~197 значений справочника стран в БД.

## Публикация новой версии

```bash
# Через переменные окружения
NEXUS_URL=http://nexus:8082 \
NEXUS_USERNAME=admin \
NEXUS_PASSWORD=secret \
./gradlew :person-service-sdk:publish

# Или через параметры Gradle
./gradlew :person-service-sdk:publish \
  -PnexusUrl=http://nexus:8082 \
  -PnexusUsername=admin \
  -PnexusPassword=secret
```

Версия `*-SNAPSHOT` публикуется в `maven-snapshots`, без суффикса — в `maven-releases`.

## Генерация кода

SDK генерируется из [`openapi/person-service-api.yaml`](../openapi/person-service-api.yaml) плагином OpenAPI Generator:

```kotlin
generatorName = "spring"
library = "spring-http-interface"  // Spring HTTP Interface (не WebClient напрямую)
reactive = true                    // Mono<ResponseEntity<T>> вместо ResponseEntity<T>
interfaceOnly = true               // Только интерфейс, без реализации
```
