package com.ersted.service;

import com.ersted.client.KeycloakClient;
import com.ersted.dto.*;
import com.ersted.exception.ValidationException;
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
    private final UserMetrics userMetrics;


    public Mono<TokenResponse> login(UserLoginRequest userLoginRequest) {
        return tokenService.login(userLoginRequest.getEmail(), userLoginRequest.getPassword())
                .doOnSuccess(_ -> userMetrics.recordLoginSuccess())
                .doOnError(_ -> userMetrics.recordLoginFailure());
    }

    public Mono<TokenResponse> refreshToken(TokenRefreshRequest tokenRefreshRequest) {
        return tokenService.refreshToken(tokenRefreshRequest.getRefreshToken())
                .doOnSuccess(_ -> userMetrics.recordRefreshTokenSuccess())
                .doOnError(_ -> userMetrics.recordRefreshTokenFailure());
    }


    public Mono<TokenResponse> register(UserRegistrationRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            userMetrics.recordRegisterFailure();
            return Mono.error(new ValidationException("Passwords do not match"));
        }

        return keycloakClient.createUser(request.getEmail(), request.getPassword())
                .doOnSubscribe(_ -> log.info("User registration: {}", request.getEmail()))
                .then(Mono.defer(() -> tokenService.login(request.getEmail(), request.getPassword())))
                .doOnSuccess(_ -> {
                    userMetrics.recordRegisterSuccess();
                    log.info("User registered successfully: {}", request.getEmail());
                })
                .doOnError(_ -> {
                    userMetrics.recordRegisterFailure();
                    log.warn("Registration failed for user: {}", request.getEmail());
                });
    }

    public Mono<UserInfoResponse> fetchUserInfo(String token) {
        return tokenService.getUserInfo(token)
                .doOnSuccess(_ -> userMetrics.recordGetInfoSuccess())
                .doOnError(_ -> userMetrics.recordGetInfoFailure());
    }

}
