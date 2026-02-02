package com.ersted.controller;

import com.ersted.client.KeycloakClient;
import com.ersted.config.KeycloakTestContainer;
import com.ersted.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthControllerTest implements KeycloakTestContainer {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private KeycloakClient keycloakClient;

    @Test
    void shouldRegistrationFlowSuccessfully() {
        var email = "testSuccessfully@test.com";
        var password = "password123";

        var registrationRequest = new UserRegistrationRequest(email, password, password);

        WebTestClient.ResponseSpec exchange = webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registrationRequest)
                .exchange();

        exchange
                .expectStatus().isCreated()
                .expectBody(TokenResponse.class)
                .value(response -> {
                    assertNotNull(response.getAccessToken());
                    assertNotNull(response.getRefreshToken());
                    assertEquals(300, response.getExpiresIn());
                    assertEquals("Bearer", response.getTokenType());
                });
    }

    @Test
    void shouldConfirmPasswordNotMatchFailed() {
        var email = "testPasswordNotMatch@test.com";
        var password = "password123";
        var differentConfirmPassword = "password";

        var registrationRequest = new UserRegistrationRequest(email, password, differentConfirmPassword);

        WebTestClient.ResponseSpec exchange = webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registrationRequest)
                .exchange();

        exchange
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(400, response.getStatus());
                    assertNotNull(response.getError());
                });
    }

    @Test
    void shouldCreatedUserAlreadyExistFailed() {
        var email = "test@test.com";
        var password = "password123";

        keycloakClient.createUser(email, password).block();
        var registrationRequest = new UserRegistrationRequest(email, password, password);

        WebTestClient.ResponseSpec exchange = webTestClient.post()
                .uri("/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registrationRequest)
                .exchange();

        exchange
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(409, response.getStatus());
                    assertNotNull(response.getError());
                });
    }


    @Test
    void shouldLoginFlowSuccessfully() {
        var email = "loginflowuser@test.com";
        var password = "password123";

        keycloakClient.createUser(email, password).block();
        var loginRequest = new UserLoginRequest(email, password);


        WebTestClient.ResponseSpec exchange = webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange();

        exchange
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .value(response -> {
                    assertNotNull(response.getAccessToken());
                    assertNotNull(response.getRefreshToken());
                    assertEquals(300, response.getExpiresIn());
                    assertEquals("Bearer", response.getTokenType());
                });
    }

    @Test
    void shouldLoginFlowFailed() {
        var email = "loginflowuserFailed@test.com";
        var password = "password123";
        var wrongPassword = "wrongPassword";

        keycloakClient.createUser(email, password).block();
        var loginRequest = new UserLoginRequest(email, wrongPassword);


        WebTestClient.ResponseSpec exchange = webTestClient.post()
                .uri("/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange();

        exchange
                .expectStatus().isUnauthorized()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(401, response.getStatus());
                    assertNotNull(response.getError());
                });
    }

    @Test
    void shouldRefreshTokenFlowSuccessfully() {
        var email = "refreshtokenflow@test.com";
        var password = "password123";

        keycloakClient.createUser(email, password).block();
        var token = keycloakClient.requestToken(email, password).block();
        var tokenRefreshRequest = new TokenRefreshRequest(token.getRefreshToken());

        WebTestClient.ResponseSpec exchange = webTestClient.post()
                .uri("/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tokenRefreshRequest)
                .exchange();

        exchange
                .expectStatus().isOk()
                .expectBody(TokenResponse.class)
                .value(response -> {
                    assertNotNull(response.getAccessToken());
                    assertNotEquals(token.getAccessToken(), response.getAccessToken());

                    assertNotNull(response.getRefreshToken());
                    assertNotEquals(token.getRefreshToken(), response.getRefreshToken());

                    assertEquals(300, response.getExpiresIn());
                    assertEquals("Bearer", response.getTokenType());
                });
    }

    @Test
    void shouldWrongRefreshTokenFailed() {
        var tokenRefreshRequest = new TokenRefreshRequest("wrong-refresh-token");

        WebTestClient.ResponseSpec exchange = webTestClient.post()
                .uri("/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tokenRefreshRequest)
                .exchange();

        exchange
                .expectStatus().isUnauthorized()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(401, response.getStatus());
                    assertNotNull(response.getError());
                });
    }

    @Test
    void shouldTakenUserInfoFlowSuccessfully() {
        var email = "userinfoflow@test.com";
        var password = "password123";

        keycloakClient.createUser(email, password).block();
        var token = keycloakClient.requestToken(email, password).block();

        WebTestClient.ResponseSpec exchange = webTestClient.get()
                .uri("/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                .exchange();

        exchange
                .expectStatus().isOk()
                .expectBody(UserInfoResponse.class)
                .value(response -> {
                    assertNotNull(response.getId());
                    assertNotNull(response.getCreatedAt());
                    assertEquals(email, response.getEmail());
                    assertFalse(response.getRoles().isEmpty());
                });
    }

    @Test
    void shouldInvalidAccessTokenSuccessfully() throws InterruptedException {
        var invalidAccessToken = "Bearer invalid-access-token";
        WebTestClient.ResponseSpec exchange = webTestClient.get()
                .uri("/v1/auth/me")
                .header(HttpHeaders.AUTHORIZATION, invalidAccessToken)
                .exchange();

        exchange
                .expectStatus().isUnauthorized()
                .expectBody(ErrorResponse.class)
                .value(response -> {
                    assertEquals(401, response.getStatus());
                    assertNotNull(response.getError());
                });
    }

}