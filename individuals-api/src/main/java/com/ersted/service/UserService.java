package com.ersted.service;

import com.ersted.annotation.Counted;
import com.ersted.client.KeycloakClient;
import com.ersted.dto.*;
import com.ersted.exception.ValidationException;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final KeycloakClient keycloakClient;
    private final TokenService tokenService;

    @Counted
    @WithSpan("userService.login")
    public Mono<TokenResponse> login(UserLoginRequest userLoginRequest) {
        return tokenService.login(userLoginRequest.getEmail(), userLoginRequest.getPassword());
    }

    @Counted
    @WithSpan("userService.refreshToken")
    public Mono<TokenResponse> refreshToken(TokenRefreshRequest tokenRefreshRequest) {
        return tokenService.refreshToken(tokenRefreshRequest.getRefreshToken());
    }

    @Counted
    @WithSpan("userService.register")
    public Mono<TokenResponse> register(UserRegistrationRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return Mono.error(new ValidationException("Passwords do not match"));
        }

        return keycloakClient.createUser(request.getEmail(), request.getPassword())
                .doOnSubscribe(_ -> log.info("User registration: {}", request.getEmail()))
                .then(Mono.defer(() -> tokenService.login(request.getEmail(), request.getPassword())))
                .doOnSuccess(_ -> log.info("User registered successfully: {}", request.getEmail()))
                .doOnError(e -> log.warn("Registration failed for user: {}", request.getEmail()));
    }

    @WithSpan("userService.fetchUserInfo")
    public Mono<UserInfoResponse> fetchUserInfo(String token) {
        return tokenService.getUserInfo(token);
    }

}
