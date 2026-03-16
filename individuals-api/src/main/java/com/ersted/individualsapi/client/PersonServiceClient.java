package com.ersted.individualsapi.client;

import com.ersted.personservice.sdk.api.IndividualsApi;
import com.ersted.personservice.sdk.model.IndividualCreateProfileRequest;
import com.ersted.personservice.sdk.model.IndividualInfoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonServiceClient {

    private final IndividualsApi personServiceApi;


    public Mono<IndividualInfoResponse> createProfile(IndividualCreateProfileRequest request) {
        return personServiceApi.createIndividual(Mono.just(request))
                .flatMap(responseEntity -> Mono.justOrEmpty(responseEntity.getBody()))
                .switchIfEmpty(Mono.error(new IllegalStateException("Empty response")))
                .doOnSubscribe(_ -> log.info("Creating individual profile"))
                .doOnSuccess(info -> log.info("Individual profile created: {}", info.getId()));
    }

    public Mono<Void> purgeProfile(UUID individualId) {
        return personServiceApi.purgeIndividual(individualId)
                .flatMap(responseEntity -> Mono.justOrEmpty(responseEntity.getBody()))
                .doOnSubscribe(_ -> log.info("Purging individual profile: {}", individualId))
                .doOnSuccess(_ -> log.info("Individual profile purged: {}", individualId));
    }

    public Mono<Void> activateProfile(UUID individualId) {
        return personServiceApi.activateIndividual(individualId)
                .flatMap(responseEntity -> Mono.justOrEmpty(responseEntity.getBody()))
                .doOnSubscribe(_ -> log.info("Activating individual profile: {}", individualId))
                .doOnSuccess(_ -> log.info("Individual profile activated: {}", individualId));
    }

}
