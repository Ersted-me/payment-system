package com.ersted.client;

import com.ersted.exception.InvalidCredentialsException;
import com.ersted.exception.KeycloakClientConflictException;
import com.ersted.exception.handler.KeycloakErrorHandler;
import com.ersted.provider.KeycloakAdminTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeycloakClientTest {

    private static final String CLIENT_ID = "test-client-id";
    private static final String CLIENT_SECRET = "test-client-secret";

    private MockWebServer mockWebServer;
    private KeycloakClient keycloakClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("").toString().replaceAll("/$", "");

        var settings = new KeycloakClientSettings(
                new KeycloakClientSettings.ClientCredentials(CLIENT_ID, CLIENT_SECRET),
                baseUrl + "/realms/test-realm/protocol/openid-connect/token",
                baseUrl + "/admin/realms/test-realm/users",
                new KeycloakClientSettings.RetrySettings(0, Duration.ZERO, Duration.ofSeconds(5))
        );

        var webClient = WebClient.create();
        var errorHandler = new KeycloakErrorHandler(new ObjectMapper());
        var tokenProvider = new KeycloakAdminTokenProvider(60, 300);

        keycloakClient = new KeycloakClient(webClient, settings, errorHandler, tokenProvider);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldRequestTokenSuccessfully() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                          "access_token": "access-token",
                          "refresh_token": "refresh-token",
                          "expires_in": 300,
                          "token_type": "Bearer"
                        }
                        """));

        // When & Then
        StepVerifier.create(keycloakClient.requestToken("test@test.com", "password"))
                .assertNext(response -> {
                    assertEquals("access-token", response.getAccessToken());
                    assertEquals("refresh-token", response.getRefreshToken());
                    assertEquals(300, response.getExpiresIn());
                    assertEquals("Bearer", response.getTokenType());
                })
                .verifyComplete();

        // Verify request
        RecordedRequest request = mockWebServer.takeRequest();
        String requestBody = URLDecoder.decode(request.getBody().readUtf8(), StandardCharsets.UTF_8);

        assertEquals("POST", request.getMethod());
        assertTrue(requestBody.contains("grant_type=password"));
        assertTrue(requestBody.contains("client_id=" + CLIENT_ID));
        assertTrue(requestBody.contains("client_secret=" + CLIENT_SECRET));
        assertTrue(requestBody.contains("username=test@test.com"));
        assertTrue(requestBody.contains("password=password"));
        assertTrue(requestBody.contains("scope=openid email profile"));
    }

    @Test
    void shouldRefreshTokenSuccessfully() throws InterruptedException {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                          "access_token": "new-access-token",
                          "refresh_token": "new-refresh-token",
                          "expires_in": 300,
                          "token_type": "Bearer"
                        }
                        """));

        // When & Then
        StepVerifier.create(keycloakClient.refreshToken("old-refresh-token"))
                .assertNext(response -> {
                    assertEquals("new-access-token", response.getAccessToken());
                    assertEquals("new-refresh-token", response.getRefreshToken());
                    assertEquals(300, response.getExpiresIn());
                    assertEquals("Bearer", response.getTokenType());
                })
                .verifyComplete();

        // Verify request
        RecordedRequest request = mockWebServer.takeRequest();
        String requestBody = URLDecoder.decode(request.getBody().readUtf8(), StandardCharsets.UTF_8);

        assertEquals("POST", request.getMethod());
        assertTrue(requestBody.contains("grant_type=refresh_token"));
        assertTrue(requestBody.contains("client_id=" + CLIENT_ID));
        assertTrue(requestBody.contains("client_secret=" + CLIENT_SECRET));
        assertTrue(requestBody.contains("refresh_token=old-refresh-token"));
    }

    @Test
    void shouldCreateUserSuccessfully() throws InterruptedException {
        // Given - admin token response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                          "access_token": "admin-access-token",
                          "expires_in": 300,
                          "token_type": "Bearer"
                        }
                        """));

        // Given - create user response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201));

        // When & Then
        StepVerifier.create(keycloakClient.createUser("test@test.com", "password"))
                .verifyComplete();

        // Verify requests
        assertEquals(2, mockWebServer.getRequestCount());

        // Admin token request
        RecordedRequest adminTokenRequest = mockWebServer.takeRequest();
        String adminTokenRequestBody = URLDecoder.decode(adminTokenRequest.getBody().readUtf8(), StandardCharsets.UTF_8);

        assertEquals("POST", adminTokenRequest.getMethod());
        assertTrue(adminTokenRequestBody.contains("grant_type=client_credentials"));
        assertTrue(adminTokenRequestBody.contains("client_id=" + CLIENT_ID));
        assertTrue(adminTokenRequestBody.contains("client_secret=" + CLIENT_SECRET));

        // Create user request
        RecordedRequest createUserRequest = mockWebServer.takeRequest();
        String createUserRequestBody = createUserRequest.getBody().readUtf8();

        assertEquals("POST", createUserRequest.getMethod());
        assertEquals("Bearer admin-access-token", createUserRequest.getHeader(HttpHeaders.AUTHORIZATION));
        assertTrue(createUserRequestBody.contains("\"username\":\"test@test.com\""));
        assertTrue(createUserRequestBody.contains("\"email\":\"test@test.com\""));
        assertTrue(createUserRequestBody.contains("\"value\":\"password\""));
    }

    @Test
    void shouldFailOnInvalidCredentials() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(400)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                          "error": "invalid_grant",
                          "error_description": "Invalid user credentials"
                        }
                        """));

        // When & Then
        StepVerifier.create(keycloakClient.requestToken("wrong@test.com", "wrong-password"))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void shouldFailWhenUserAlreadyExists() {
        // Given - admin token response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                          "access_token": "admin-token",
                          "expires_in": 300
                        }
                        """));

        // Given - conflict response
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(409)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                          "errorMessage": "User exists with same email"
                        }
                        """));

        // When & Then
        StepVerifier.create(keycloakClient.createUser("existing@test.com", "password"))
                .expectError(KeycloakClientConflictException.class)
                .verify();
    }
}
