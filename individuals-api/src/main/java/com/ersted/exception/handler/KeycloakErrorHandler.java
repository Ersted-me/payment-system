package com.ersted.exception.handler;

import com.ersted.exception.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;


@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakErrorHandler {

    private final ObjectMapper objectMapper;

    public <T> Mono<T> handle(Mono<T> mono) {
        return mono
                .onErrorResume(WebClientResponseException.BadRequest.class, e ->
                        Mono.error(parse400Error(e)))
                .onErrorResume(WebClientResponseException.Unauthorized.class, e -> {
                    log.error("Keycloak unauthorized: {}", e.getResponseBodyAsString());
                    return Mono.error(new KeycloakClientUnauthorizedException("Authentication failed"));
                })
                .onErrorResume(WebClientResponseException.Forbidden.class, e -> {
                    log.error("Keycloak forbidden: {}", e.getResponseBodyAsString());
                    return Mono.error(new KeycloakClientForbiddenException("Access denied"));
                })
                .onErrorResume(WebClientResponseException.Conflict.class, e -> {
                    log.warn("Keycloak conflict: {}", e.getResponseBodyAsString());
                    return Mono.error(new KeycloakClientConflictException("Resource already exists"));
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().is5xxServerError()) {
                        log.error("Keycloak server error: {}", e.getStatusCode());
                        return Mono.error(new KeycloakClientServiceUnavailableException("Keycloak unavailable"));
                    }
                    log.error("Keycloak error: {}", e.getResponseBodyAsString());
                    return Mono.error(new KeycloakClientException("Keycloak error"));
                });
    }

    private Exception parse400Error(WebClientResponseException e) {
        try {
            KeycloakErrorResponse error = objectMapper.readValue(
                    e.getResponseBodyAsString(),
                    KeycloakErrorResponse.class
            );
            log.error("Keycloak error: {} - {}", error.getError(), error.getErrorDescription());
            return mapKeycloakError(error);
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse Keycloak error: {}", e.getResponseBodyAsString());
            return new KeycloakClientBadRequestException("Invalid request");
        }
    }

    private Exception mapKeycloakError(KeycloakErrorResponse error) {
        return switch (error.getError()) {
            case "invalid_grant" -> new InvalidCredentialsException(error.getErrorDescription());
            case "invalid_client" -> new InvalidClientException(error.getErrorDescription());
            case "unauthorized_client" -> new UnauthorizedException(error.getErrorDescription());
            case "invalid_scope" -> new InvalidScopeException(error.getErrorDescription());
            default -> new KeycloakClientBadRequestException(error.getErrorDescription());
        };
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class KeycloakErrorResponse {
        private String error;
        @JsonProperty("error_description")
        private String errorDescription;
    }

}
