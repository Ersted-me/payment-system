package com.ersted.controller;

import com.ersted.api.AuthApi;
import com.ersted.dto.*;
import com.ersted.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;


@RequestMapping("/v1")
@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final UserService userService;

    @Override
    public Mono<ResponseEntity<TokenResponse>> authLoginPost(Mono<UserLoginRequest> userLoginRequest, ServerWebExchange exchange) {
        return userLoginRequest
                .flatMap(userService::login)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<UserInfoResponse>> authMeGet(ServerWebExchange exchange) {
        String token = exchange
                .getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        return userService.fetchUserInfo(token)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TokenResponse>> authRefreshTokenPost(Mono<TokenRefreshRequest> tokenRefreshRequest, ServerWebExchange exchange) {
        return tokenRefreshRequest
                .flatMap(userService::refreshToken)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<TokenResponse>> authRegistrationPost(Mono<UserRegistrationRequest> userRegistrationRequest, ServerWebExchange exchange) {
        return userRegistrationRequest
                .flatMap(userService::register)
                .map(body -> ResponseEntity
                        .created(URI.create("/v1/auth/me"))
                        .body(body)
                );
    }

}
