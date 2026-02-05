package com.ersted.client;

import com.ersted.dto.CreateKeycloakUserRequest;
import com.ersted.dto.TokenResponse;
import com.ersted.exception.hendler.KeycloakErrorHandler;
import com.ersted.provider.KeycloakAdminTokenProvider;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.annotation.NewSpan;
import io.netty.handler.timeout.TimeoutException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.naming.ServiceUnavailableException;
import java.net.ConnectException;
import java.time.Duration;
import java.util.List;

@Slf4j
@Validated
@Component
@RequiredArgsConstructor
public class KeycloakClient {

    private static final String GRANT_TYPE_PASSWORD = "password";
    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    private static final String SCOPE_OPENID = "openid email profile";

    private final int RETRY_ATTEMPTS;
    private final Duration RETRY_DELAY;
    private final Duration REQUEST_TIMEOUT;

    private final String KEYCLOAK_CLIENT_ID;
    private final String KEYCLOAK_CLIENT_SECRET;

    private final String ADMIN_USERS_URI;
    private final String TOKEN_URI;

    private final WebClient keycloakWebClient;
    private final KeycloakErrorHandler errorHandler;
    private final KeycloakAdminTokenProvider adminTokenProvider;

    @NewSpan("keycloak-request-token")
    public Mono<TokenResponse> requestToken(@NotNull @Email String email, @NotNull String password) {
        var formData = buildPasswordGrantFormData(email, password);
        return executeTokenRequest(formData)
                .doOnSubscribe(_ -> log.info("Authenticating user: {}", email))
                .doOnSuccess(_ -> log.info("User authenticated: {}", email));
    }

    @NewSpan("keycloak-refresh-token")
    public Mono<TokenResponse> refreshToken(@NotNull String refreshToken) {
        var formData = buildRefreshTokenGrantFormData(refreshToken);
        return executeTokenRequest(formData)
                .doOnSubscribe(_ -> log.debug("Refreshing token: ...{}", StringUtils.right(refreshToken, 5)))
                .doOnSuccess(_ -> log.info("Token refreshed successfully"));
    }

    @NewSpan("keycloak-create-user")
    public Mono<Void> createUser(@NotNull @Email String email, @NotNull String password) {
        return adminToken()
                .doOnSubscribe(_ -> log.info("Creating user: {}", email))
                .flatMap(token -> executeCreateUserRequest(email, password, token.getAccessToken()))
                .doOnSuccess(_ -> log.info("User created successfully: {}", email));
    }

    private Mono<TokenResponse> adminToken() {
        return adminTokenProvider.getToken(this::fetchAdminToken);
    }

    private Mono<TokenResponse> fetchAdminToken() {
        return executeTokenRequest(buildClientCredentialsGrantFormData())
                .doOnSubscribe(_ -> log.debug("Requesting admin token"))
                .doOnSuccess(_ -> log.info("Admin token obtained"));
    }

    private Mono<Void> executeCreateUserRequest(
            @NotNull @Email String email,
            @NotNull String password,
            @NotNull String accessToken) {

        var request = buildCreateUserRequest(email, password);

        return this.retrieve(
                keycloakWebClient
                        .post()
                        .uri(ADMIN_USERS_URI)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request),
                Void.class);
    }

    private Mono<TokenResponse> executeTokenRequest(MultiValueMap<String, String> formData) {
        return this.retrieve(
                keycloakWebClient
                        .post()
                        .uri(TOKEN_URI)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .bodyValue(formData),
                TokenResponse.class
        );
    }

    private MultiValueMap<String, String> buildClientCredentialsGrantFormData() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", GRANT_TYPE_CLIENT_CREDENTIALS);
        formData.add("client_id", KEYCLOAK_CLIENT_ID);
        formData.add("client_secret", KEYCLOAK_CLIENT_SECRET);
        return formData;
    }

    private MultiValueMap<String, String> buildPasswordGrantFormData(String username, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", GRANT_TYPE_PASSWORD);
        formData.add("client_id", KEYCLOAK_CLIENT_ID);
        formData.add("client_secret", KEYCLOAK_CLIENT_SECRET);
        formData.add("username", username);
        formData.add("password", password);
        formData.add("scope", SCOPE_OPENID);
        return formData;
    }

    private MultiValueMap<String, String> buildRefreshTokenGrantFormData(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", GRANT_TYPE_REFRESH_TOKEN);
        formData.add("client_id", KEYCLOAK_CLIENT_ID);
        formData.add("client_secret", KEYCLOAK_CLIENT_SECRET);
        formData.add("refresh_token", refreshToken);
        return formData;
    }

    private <T> Mono<T> retrieve(WebClient.RequestHeadersSpec<?> spec, Class<T> responseType) {
        return spec.retrieve()
                .bodyToMono(responseType)
                .transform(errorHandler::handle)
                .timeout(REQUEST_TIMEOUT)
                .retryWhen(Retry.backoff(RETRY_ATTEMPTS, RETRY_DELAY)
                        .filter(this::isRetryableError)
                        .doBeforeRetry(signal ->
                                log.warn("Retrying request, attempt: {}", signal.totalRetries() + 1))
                );
    }

    private boolean isRetryableError(Throwable throwable) {
        return throwable instanceof ServiceUnavailableException ||
                throwable instanceof TimeoutException ||
                throwable instanceof ConnectException;
    }

    private CreateKeycloakUserRequest buildCreateUserRequest(String email, String password) {
        return CreateKeycloakUserRequest.builder()
                .username(email)
                .email(email)
                .emailVerified(true)
                .enabled(true)
                .requiredActions(List.of())
                .credentials(List.of(
                        CreateKeycloakUserRequest.Credential.builder()
                                .type("password")
                                .value(password)
                                .temporary(false)
                                .build()
                ))
                .build();
    }

}