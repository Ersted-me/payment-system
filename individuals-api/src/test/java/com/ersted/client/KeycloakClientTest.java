package com.ersted.client;

import com.ersted.config.KeycloakProperties;
import com.ersted.dto.TokenResponse;
import com.ersted.exception.InvalidCredentialsException;
import com.ersted.exception.KeycloakClientConflictException;
import com.ersted.exception.hendler.KeycloakErrorHandler;
import com.ersted.provider.KeycloakAdminTokenProvider;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.MediaType;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class KeycloakClientTest {

    private MockWebServer mockWebServer;
    private KeycloakClient keycloakClient;

    @Mock
    private KeycloakErrorHandler errorHandler;

    @Mock
    private KeycloakAdminTokenProvider provider;

    private KeycloakProperties properties;


    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        properties = new KeycloakProperties();
        properties.setUrl(mockWebServer.url("/").toString());
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setRealm("client-realm");

        WebClient webClient = WebClient.builder().build();

        keycloakClient = new KeycloakClient(webClient, properties, errorHandler, provider);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }


    @Test
    void shouldRequestTokenSuccessfully() throws InterruptedException {
        // Given
        String responseBody = """
                {
                  "access_token": "access-token",
                  "refresh_token": "refresh-token",
                  "expires_in": 300,
                  "token_type": "Bearer"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        );

        // When
        when(errorHandler.handle(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(keycloakClient.requestToken("test@test.com", "password"))
                // Then
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
        assertTrue(requestBody.contains("client_id=" + properties.getClientId()));
        assertTrue(requestBody.contains("client_secret=" + properties.getClientSecret()));
        assertTrue(requestBody.contains("username=test@test.com"));
        assertTrue(requestBody.contains("password=password"));
        assertTrue(requestBody.contains("scope=openid email profile"));
    }


    @Test
    void shouldRefreshTokenSuccessfully() throws InterruptedException {
        // Given
        String responseBody = """
                {
                  "access_token": "new-access-token",
                  "refresh_token": "new-refresh-token",
                  "expires_in": 300,
                  "token_type": "Bearer"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseBody)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        );

        // When
        when(errorHandler.handle(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(keycloakClient.refreshToken("old-refresh-token"))
                // Then
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
        assertTrue(requestBody.contains("client_id=" + properties.getClientId()));
        assertTrue(requestBody.contains("client_secret=" + properties.getClientSecret()));
        assertTrue(requestBody.contains("refresh_token=old-refresh-token"));
    }

    @Test
    void shouldCreateUserSuccessfully() throws InterruptedException {
        // Given
        String responseAdminToken = """
                {
                  "access_token": "admin-access-token",
                  "expires_in": 300,
                  "token_type": "Bearer"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(responseAdminToken)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        );

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(201)
                .setHeader(HttpHeaders.LOCATION,
                        properties.getUrl() + "/admin/realms/" + properties.getRealm() + "/users/" + "some-uuid-id")
        );

        // When
        when(errorHandler.handle(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(provider.getToken(any())).thenAnswer(invocation -> {
            Supplier<Mono<TokenResponse>> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        StepVerifier.create(keycloakClient.createUser("test@test.com", "password"))
                .verifyComplete();


        // Verify request
        assertEquals(2, mockWebServer.getRequestCount());

        // Admin token request
        RecordedRequest adminTokenRequest = mockWebServer.takeRequest();
        String adminTokenRequestBody = URLDecoder.decode(adminTokenRequest.getBody().readUtf8(), StandardCharsets.UTF_8);

        assertEquals("POST", adminTokenRequest.getMethod());
        assertTrue(adminTokenRequestBody.contains("grant_type=client_credentials"));
        assertTrue(adminTokenRequestBody.contains("client_id=" + properties.getClientId()));
        assertTrue(adminTokenRequestBody.contains("client_secret=" + properties.getClientSecret()));

        // Create user request
        RecordedRequest createUserRequest = mockWebServer.takeRequest();
        String createUserRequestBody = URLDecoder.decode(createUserRequest.getBody().readUtf8(), StandardCharsets.UTF_8);

        assertEquals("POST", createUserRequest.getMethod());
        assertEquals("Bearer admin-access-token", createUserRequest.getHeader(HttpHeaders.AUTHORIZATION));
        assertTrue(createUserRequestBody.contains("\"username\":\"test@test.com\""));
        assertTrue(createUserRequestBody.contains("\"email\":\"test@test.com\""));
        assertTrue(createUserRequestBody.contains("\"value\":\"password\""));
    }

    @Test
    void shouldFailWhenAdminTokenRequestFails() {
        // Given - admin token request падает
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody("""
                {
                  "error": "invalid_client",
                  "error_description": "Invalid client credentials"
                }
                """));

        // When & Then
        when(provider.getToken(any())).thenAnswer(invocation -> {
            Supplier<Mono<TokenResponse>> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        when(errorHandler.handle(any())).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono.onErrorMap(WebClientResponseException.Unauthorized.class,
                    e -> new InvalidCredentialsException("Invalid client credentials"));
        });

        StepVerifier.create(keycloakClient.createUser("test@test.com", "password"))
                .expectError(InvalidCredentialsException.class)
                .verify();
    }

    @Test
    void shouldFailWhenUserAlreadyExists() {
        // Given
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody("""
                {
                  "access_token": "admin-token",
                  "expires_in": 300
                }
                """));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(409)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody("""
                {
                  "errorMessage": "User exists with same email"
                }
                """));

        // When & Then
        when(provider.getToken(any())).thenAnswer(invocation -> {
            Supplier<Mono<TokenResponse>> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        when(errorHandler.handle(any())).thenAnswer(invocation -> {
            Mono<?> mono = invocation.getArgument(0);
            return mono.onErrorMap(WebClientResponseException.Conflict.class,
                    e -> new KeycloakClientConflictException("User already exists"));
        });

        StepVerifier.create(keycloakClient.createUser("existing@test.com", "password"))
                .expectError(KeycloakClientConflictException.class)
                .verify();
    }

}