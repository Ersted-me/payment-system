package com.ersted.service;

import com.ersted.client.KeycloakClient;
import com.ersted.dto.TokenResponse;
import com.ersted.dto.UserInfoResponse;
import com.ersted.exception.KeycloakClientUnauthorizedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private KeycloakClient keycloakClient;

    @Mock
    private ReactiveJwtDecoder jwtDecoder;

    @InjectMocks
    private TokenService tokenService;

    @Test
    void shouldLoginSuccessfully() {
        // Given
        String email = "test@example.com";
        String password = "password123";
        TokenResponse expectedResponse = new TokenResponse()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .expiresIn(3600)
                .tokenType("Bearer");

        when(keycloakClient.requestToken(email, password))
                .thenReturn(Mono.just(expectedResponse));

        // When
        StepVerifier.create(tokenService.login(email, password))
                // Then
                .assertNext(response -> {
                    assertEquals("access-token", response.getAccessToken());
                    assertEquals("refresh-token", response.getRefreshToken());
                    assertEquals(3600, response.getExpiresIn());
                    assertEquals("Bearer", response.getTokenType());
                })
                .verifyComplete();

        // Verify
        verify(keycloakClient).requestToken(email, password);
    }

    @Test
    void shouldHandleLoginFailure() {
        // Given
        String email = "test@example.com";
        String password = "wrong-password";

        when(keycloakClient.requestToken(email, password))
                .thenReturn(Mono.error(new KeycloakClientUnauthorizedException("Invalid credentials")));

        // When
        StepVerifier.create(tokenService.login(email, password))
                // Then
                .expectErrorMatches(throwable ->
                        throwable instanceof KeycloakClientUnauthorizedException &&
                                throwable.getMessage().equals("Invalid credentials")
                )
                .verify();

        // Verify
        verify(keycloakClient).requestToken(email, password);
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        // Given
        String refreshToken = "valid-refresh-token";
        TokenResponse expectedResponse = new TokenResponse()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .expiresIn(3600)
                .tokenType("Bearer");

        when(keycloakClient.refreshToken(refreshToken))
                .thenReturn(Mono.just(expectedResponse));

        // When
        StepVerifier.create(tokenService.refreshToken(refreshToken))
                // Then
                .assertNext(response -> {
                    assertEquals("new-access-token", response.getAccessToken());
                    assertEquals("new-refresh-token", response.getRefreshToken());
                    assertEquals(3600, response.getExpiresIn());
                    assertEquals("Bearer", response.getTokenType());
                })
                .verifyComplete();

        // Verify
        verify(keycloakClient).refreshToken(refreshToken);
    }

    @Test
    void shouldHandleRefreshTokenFailure() {
        // Given
        String refreshToken = "invalid-refresh-token";

        when(keycloakClient.refreshToken(refreshToken))
                .thenReturn(Mono.error(new RuntimeException("Invalid refresh token")));

        // When
        StepVerifier.create(tokenService.refreshToken(refreshToken))
                // Then
                .expectErrorMatches(throwable ->
                        throwable instanceof RuntimeException &&
                                throwable.getMessage().equals("Invalid refresh token")
                )
                .verify();

        // Verify
        verify(keycloakClient).refreshToken(refreshToken);
    }

    @Test
    void shouldGetUserInfoSuccessfully() {
        // Given
        String token = "valid-jwt-token";
        String userId = "user-123";
        String email = "user@example.com";
        List<String> roles = List.of("USER", "ADMIN");
        Long createdAtTimestamp = 1640000000000L;

        Jwt jwt = createMockJwt(userId, email, roles, createdAtTimestamp);

        when(jwtDecoder.decode(token))
                .thenReturn(Mono.just(jwt));

        // When
        StepVerifier.create(tokenService.getUserInfo(token))
                // Then
                .assertNext(userInfo -> {
                    assertEquals(userId, userInfo.getId());
                    assertEquals(email, userInfo.getEmail());
                    assertEquals(roles, userInfo.getRoles());
                    assertNotNull(userInfo.getCreatedAt());
                    assertEquals(
                            OffsetDateTime.ofInstant(Instant.ofEpochMilli(createdAtTimestamp), ZoneOffset.UTC),
                            userInfo.getCreatedAt()
                    );
                })
                .verifyComplete();

        // Verify
        verify(jwtDecoder).decode(token);
    }

    @Test
    void shouldGetUserInfoWithBearerPrefix() {
        // Given
        String token = "Bearer valid-jwt-token";
        String userId = "user-456";
        String email = "test@example.com";
        List<String> roles = List.of("USER");
        Long createdAtTimestamp = 1650000000000L;

        Jwt jwt = createMockJwt(userId, email, roles, createdAtTimestamp);

        when(jwtDecoder.decode("valid-jwt-token"))
                .thenReturn(Mono.just(jwt));

        // When
        StepVerifier.create(tokenService.getUserInfo(token))
                // Then
                .assertNext(userInfo -> {
                    assertEquals(userId, userInfo.getId());
                    assertEquals(email, userInfo.getEmail());
                    assertEquals(roles, userInfo.getRoles());
                    assertNotNull(userInfo.getCreatedAt());
                })
                .verifyComplete();

        // Verify
        verify(jwtDecoder).decode("valid-jwt-token");
    }

    @Test
    void shouldGetUserInfoWithNullCreatedAt() {
        // Given
        String token = "valid-jwt-token";
        String userId = "user-789";
        String email = "nodate@example.com";
        List<String> roles = List.of("USER");

        Jwt jwt = createMockJwt(userId, email, roles, null);

        when(jwtDecoder.decode(token))
                .thenReturn(Mono.just(jwt));

        // When
        StepVerifier.create(tokenService.getUserInfo(token))
                // Then
                .assertNext(userInfo -> {
                    assertEquals(userId, userInfo.getId());
                    assertEquals(email, userInfo.getEmail());
                    assertEquals(roles, userInfo.getRoles());
                    assertNull(userInfo.getCreatedAt());
                })
                .verifyComplete();

        // Verify
        verify(jwtDecoder).decode(token);
    }

    @Test
    void shouldHandleInvalidJwtToken() {
        // Given
        String token = "invalid-jwt-token";

        when(jwtDecoder.decode(token))
                .thenReturn(Mono.error(new JwtException("Invalid JWT token")));

        // When
        StepVerifier.create(tokenService.getUserInfo(token))
                // Then
                .expectErrorMatches(throwable ->
                        throwable instanceof JwtException &&
                                throwable.getMessage().equals("Invalid JWT token")
                )
                .verify();

        // Verify
        verify(jwtDecoder).decode(token);
    }

    @Test
    void shouldHandleJwtDecodingError() {
        // Given
        String token = "Bearer malformed-token";

        when(jwtDecoder.decode("malformed-token"))
                .thenReturn(Mono.error(new JwtException("JWT decoding failed")));

        // When
        StepVerifier.create(tokenService.getUserInfo(token))
                // Then
                .expectError(JwtException.class)
                .verify();

        // Verify
        verify(jwtDecoder).decode("malformed-token");
    }

    private Jwt createMockJwt(String subject, String email, List<String> roles, Long createdAt) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", subject)
                .claim("email", email)
                .claim("roles", roles)
                .claim("created_at", createdAt)
                .build();
    }

}
