package com.ersted.individualsapi.exception.handler;


import com.ersted.individualsapi.dto.ErrorResponse;
import com.ersted.individualsapi.exception.*;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(KeycloakClientConflictException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleKeycloakClientConflictException(KeycloakClientConflictException ex) {
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(KeycloakClientUnauthorizedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleKeycloakClientUnauthorizedException(KeycloakClientUnauthorizedException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleInvalidCredentialsException(InvalidCredentialsException ex) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(KeycloakClientException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleKeycloakException(KeycloakClientException ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidationException(ValidationException ex) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(PersonServiceNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePersonServiceNotFoundException(PersonServiceNotFoundException ex) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(PersonServiceUnavailableException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePersonServiceUnavailableException(PersonServiceUnavailableException ex) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(PersonServiceException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePersonServiceException(PersonServiceException ex) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleCallNotPermittedException(CallNotPermittedException ex) {
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "Service temporarily unavailable");
    }

    @ExceptionHandler(BulkheadFullException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBulkheadFullException(BulkheadFullException ex) {
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, "Too many concurrent requests");
    }

    private Mono<ResponseEntity<ErrorResponse>> buildErrorResponse(HttpStatus status, String error) {

        log.warn("{}: {}", status, error);

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setError(error);
        errorResponse.setStatus(status.value());

        return Mono.just(ResponseEntity.status(status).body(errorResponse));
    }

}
