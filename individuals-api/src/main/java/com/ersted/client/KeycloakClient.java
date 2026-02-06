package com.ersted.client;

import com.ersted.dto.CreateKeycloakUserRequest;
import com.ersted.dto.TokenResponse;
import com.ersted.exception.KeycloakClientServiceUnavailableException;
import com.ersted.exception.handler.KeycloakErrorHandler;
import com.ersted.provider.KeycloakAdminTokenProvider;
import io.netty.handler.timeout.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class KeycloakClient {

    private static final String BEARER_TOKEN_PREFIX = "Bearer ";

    private static final String GRANT_TYPE_PASSWORD = "password";
    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";
    private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    private static final String SCOPE_OPENID = "openid email profile";

    private final WebClient keycloakWebClient;
    private final KeycloakClientSettings settings;
    private final KeycloakErrorHandler errorHandler;
    private final KeycloakAdminTokenProvider adminTokenProvider;


    public Mono<TokenResponse> requestToken(String email, String password) {
        var formData = buildPasswordGrantFormData(email, password);
        return executeTokenRequest(formData)
                .doOnSubscribe(_ -> log.info("Authenticating user: {}", email))
                .doOnSuccess(_ -> log.info("User authenticated: {}", email));
    }

    public Mono<TokenResponse> refreshToken(String refreshToken) {
        var formData = buildRefreshTokenGrantFormData(refreshToken);
        return executeTokenRequest(formData)
                .doOnSubscribe(_ -> log.debug("Refreshing token: ...{}", StringUtils.right(refreshToken, 5)))
                .doOnSuccess(_ -> log.info("Token refreshed successfully"));
    }

    public Mono<Void> createUser(String email, String password) {
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

    private Mono<Void> executeCreateUserRequest(String email, String password, String accessToken) {

        var request = buildCreateUserRequest(email, password);

        return this.retrieve(
                keycloakWebClient
                        .post()
                        .uri(settings.userRegistrationUrl())
                        .header(HttpHeaders.AUTHORIZATION, BEARER_TOKEN_PREFIX + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request),
                Void.class);
    }

    private Mono<TokenResponse> executeTokenRequest(MultiValueMap<String, String> formData) {
        return this.retrieve(
                keycloakWebClient
                        .post()
                        .uri(settings.tokenUrl())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .bodyValue(formData),
                TokenResponse.class
        );
    }

    private MultiValueMap<String, String> buildClientCredentialsGrantFormData() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", GRANT_TYPE_CLIENT_CREDENTIALS);
        formData.add("client_id", settings.credentials().clientId());
        formData.add("client_secret", settings.credentials().clientSecret());
        return formData;
    }

    private MultiValueMap<String, String> buildPasswordGrantFormData(String username, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", GRANT_TYPE_PASSWORD);
        formData.add("client_id", settings.credentials().clientId());
        formData.add("client_secret", settings.credentials().clientSecret());
        formData.add("username", username);
        formData.add("password", password);
        formData.add("scope", SCOPE_OPENID);
        return formData;
    }

    private MultiValueMap<String, String> buildRefreshTokenGrantFormData(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", GRANT_TYPE_REFRESH_TOKEN);
        formData.add("client_id", settings.credentials().clientId());
        formData.add("client_secret", settings.credentials().clientSecret());
        formData.add("refresh_token", refreshToken);
        return formData;
    }

    private <T> Mono<T> retrieve(WebClient.RequestHeadersSpec<?> spec, Class<T> responseType) {
        return spec.retrieve()
                .bodyToMono(responseType)
                .transform(errorHandler::handle)
                .timeout(settings.retry().timeout())
                .retryWhen(Retry.backoff(settings.retry().attempts(), settings.retry().delay())
                        .filter(this::isRetryableError)
                        .doBeforeRetry(signal ->
                                log.warn("Retrying request, attempt: {}", signal.totalRetries() + 1))
                );
    }

    private boolean isRetryableError(Throwable throwable) {
        return throwable instanceof KeycloakClientServiceUnavailableException ||
                throwable instanceof TimeoutException ||
                throwable instanceof java.util.concurrent.TimeoutException ||
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