package com.ersted.individualsapi.client;

import com.ersted.individualsapi.exception.handler.PersonServiceErrorHandler;
import com.ersted.personservice.sdk.api.IndividualsApi;
import com.ersted.personservice.sdk.model.IndividualCreateProfileRequest;
import com.ersted.personservice.sdk.model.IndividualInfoResponse;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class PersonServiceClient {

    private static final String RESILIENCE4J_INSTANCE_NAME = "person-service";

    private final IndividualsApi personServiceApi;
    private final PersonServiceErrorHandler errorHandler;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;

    public PersonServiceClient(
            IndividualsApi personServiceApi,
            PersonServiceErrorHandler errorHandler,
            CircuitBreakerRegistry circuitBreakerRegistry,
            BulkheadRegistry bulkheadRegistry
    ) {
        this.personServiceApi = personServiceApi;
        this.errorHandler = errorHandler;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE4J_INSTANCE_NAME);
        this.bulkhead = bulkheadRegistry.bulkhead(RESILIENCE4J_INSTANCE_NAME);
    }

    public Mono<IndividualInfoResponse> createProfile(IndividualCreateProfileRequest request) {
        return personServiceApi.createIndividual(Mono.just(request))
                .flatMap(responseEntity -> Mono.justOrEmpty(responseEntity.getBody()))
                .switchIfEmpty(Mono.error(new IllegalStateException("Empty response")))
                .as(this::withResilience)
                .doOnSubscribe(_ -> log.info("Creating individual profile"))
                .doOnSuccess(info -> log.info("Individual profile created: {}", info.getId()));
    }

    public Mono<Void> purgeProfile(UUID individualId) {
        return personServiceApi.purgeIndividual(individualId)
                .flatMap(responseEntity -> Mono.justOrEmpty(responseEntity.getBody()))
                .as(this::withResilience)
                .doOnSubscribe(_ -> log.info("Purging individual profile: {}", individualId))
                .doOnSuccess(_ -> log.info("Individual profile purged: {}", individualId));
    }

    public Mono<Void> activateProfile(UUID individualId) {
        return personServiceApi.activateIndividual(individualId)
                .flatMap(responseEntity -> Mono.justOrEmpty(responseEntity.getBody()))
                .as(this::withResilience)
                .doOnSubscribe(_ -> log.info("Activating individual profile: {}", individualId))
                .doOnSuccess(_ -> log.info("Individual profile activated: {}", individualId));
    }

    private <T> Mono<T> withResilience(Mono<T> mono) {
        return mono
                .transform(errorHandler::handle)
                .transformDeferred(BulkheadOperator.of(bulkhead))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
    }

}
