package com.ersted.spec.service;

import com.ersted.client.KeycloakClient;
import com.ersted.dto.TokenResponse;
import com.ersted.dto.UserRegistrationRequest;
import com.ersted.exception.KeycloakClientConflictException;
import com.ersted.exception.ValidationException;
import com.ersted.service.TokenService;
import com.ersted.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private KeycloakClient client;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserMetrics userMetrics;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterSuccessfully() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest(
                "test@test.com",
                "password",
                "password"
        );

        TokenResponse expectedToken = new TokenResponse();
        expectedToken.setAccessToken("access-token");
        expectedToken.setRefreshToken("refresh-token");
        expectedToken.setExpiresIn(300);
        expectedToken.setTokenType("Bearer");

        when(client.createUser(request.getEmail(), request.getPassword()))
                .thenReturn(Mono.empty());
        when(tokenService.login(request.getEmail(), request.getPassword()))
                .thenReturn(Mono.just(expectedToken));

        // When
        StepVerifier.create(userService.register(request))
                // Then
                .assertNext(response -> {
                    assertEquals("access-token", response.getAccessToken());
                    assertEquals("refresh-token", response.getRefreshToken());
                    assertEquals(300, response.getExpiresIn());
                    assertEquals("Bearer", response.getTokenType());
                })
                .verifyComplete();

        // Verify
        verify(client).createUser("test@test.com", "password");
        verify(tokenService).login("test@test.com", "password");
    }

    @Test
    void shouldFailWhenPasswordsDontMatch() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@test.com");
        request.setPassword("password");
        request.setConfirmPassword("different-password");

        // When
        StepVerifier.create(userService.register(request))
                // Then
                .expectErrorMatches(throwable ->
                        throwable instanceof ValidationException &&
                                throwable.getMessage().equals("Passwords do not match")
                )
                .verify();

        verifyNoInteractions(client);
        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldFailWhenUserAlreadyExists() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("existing@test.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        when(client.createUser(request.getEmail(), request.getPassword()))
                .thenReturn(Mono.error(new KeycloakClientConflictException("User already exists")));

        // When
        StepVerifier.create(userService.register(request))
                // Then
                .expectError(KeycloakClientConflictException.class)
                .verify();

        verify(client).createUser("existing@test.com", "password123");
        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldFailWhenCreateUserThrowsError() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        when(client.createUser(request.getEmail(), request.getPassword()))
                .thenReturn(Mono.error(new RuntimeException("Keycloak error")));

        // When
        StepVerifier.create(userService.register(request))
                .expectError(RuntimeException.class)
                .verify();

        verify(client).createUser("test@test.com", "password123");
        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldFailWhenLoginFails() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        when(client.createUser(anyString(), anyString()))
                .thenReturn(Mono.empty());

        when(tokenService.login(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Login failed")));

        // When
        StepVerifier.create(userService.register(request))
                .expectError(RuntimeException.class)
                .verify();

        verify(client).createUser("test@test.com", "password123");
        verify(tokenService).login("test@test.com", "password123");
    }

}