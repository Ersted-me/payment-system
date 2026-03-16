package com.ersted.individualsapi.exception.handler;

import com.ersted.individualsapi.exception.PersonServiceException;
import com.ersted.individualsapi.exception.PersonServiceNotFoundException;
import com.ersted.individualsapi.exception.PersonServiceUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class PersonServiceErrorHandler {

    public <T> Mono<T> handle(Mono<T> mono) {
        return mono
                .onErrorResume(WebClientResponseException.NotFound.class, e -> {
                    log.warn("Person-service resource not found: {}", e.getMessage());
                    return Mono.error(new PersonServiceNotFoundException("Individual not found"));
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().is5xxServerError()) {
                        log.error("Person-service server error: {}", e.getStatusCode());
                        return Mono.error(new PersonServiceUnavailableException("Person service unavailable"));
                    }
                    log.error("Person-service error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
                    return Mono.error(new PersonServiceException("Person service error"));
                });
    }

}
