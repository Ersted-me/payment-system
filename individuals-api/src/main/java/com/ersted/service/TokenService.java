package com.ersted.service;

import com.ersted.client.KeycloakClient;
import com.ersted.dto.TokenResponse;
import com.ersted.dto.UserInfoResponse;
import com.ersted.utils.DateTimeUtils;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import reactor.core.publisher.Mono;

@Slf4j
@Validated
@Service
@RequiredArgsConstructor
public class TokenService {

    private final KeycloakClient keycloakClient;
    private final ReactiveJwtDecoder jwtDecoder;

    @WithSpan("tokenService.login")
    public Mono<TokenResponse> login(@NotNull @Email String email, @NotNull String password) {
        return keycloakClient.requestToken(email, password)
                .doOnSuccess(_ -> log.info("User logged in: {}", email))
                .doOnError(e -> log.warn("Login failed for user: {}", email));
    }

    @WithSpan("tokenService.refreshToken")
    public Mono<TokenResponse> refreshToken(String refreshToken) {
        return keycloakClient.refreshToken(refreshToken);
    }

    @WithSpan("tokenService.getUserInfo")
    public Mono<UserInfoResponse> getUserInfo(@NotNull String token) {
        String cleanToken = token.startsWith("Bearer ")
                ? token.substring(7)
                : token;

        return jwtDecoder.decode(cleanToken)
                .map(this::mapToUserInfo)
                .doOnError(e -> log.error("Failed to decode JWT token", e));
    }

    private UserInfoResponse mapToUserInfo(Jwt jwt) {
        Long createdAtTimestamp = jwt.getClaim("created_at");

        return new UserInfoResponse()
                .id(jwt.getSubject())
                .email(jwt.getClaim("email"))
                .roles(jwt.getClaim("roles"))
                .createdAt(createdAtTimestamp != null
                        ? DateTimeUtils.offsetDateTimeFromLong(createdAtTimestamp)
                        : null);
    }

}
