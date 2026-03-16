package com.ersted.individualsapi.spec.service;

import com.ersted.individualsapi.client.KeycloakClient;
import com.ersted.individualsapi.client.PersonServiceClient;
import com.ersted.individualsapi.dto.TokenResponse;
import com.ersted.individualsapi.dto.UserRegistrationRequest;
import com.ersted.individualsapi.exception.KeycloakClientConflictException;
import com.ersted.individualsapi.exception.ValidationException;
import com.ersted.individualsapi.mapper.IndividualMapper;
import com.ersted.individualsapi.service.TokenService;
import com.ersted.individualsapi.service.UserService;
import com.ersted.personservice.sdk.model.IndividualCreateProfileRequest;
import com.ersted.personservice.sdk.model.IndividualInfoResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private KeycloakClient client;

    @Mock
    private PersonServiceClient personServiceClient;

    @Mock
    private IndividualMapper individualMapper;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldRegisterSuccessfully() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest(
                "test@test.com",
                "password",
                "password",
                null
        );

        IndividualInfoResponse individualInfo = new IndividualInfoResponse();
        individualInfo.setId(UUID.randomUUID());
        individualInfo.setUserId(UUID.randomUUID());

        TokenResponse expectedToken = new TokenResponse();
        expectedToken.setAccessToken("access-token");
        expectedToken.setRefreshToken("refresh-token");
        expectedToken.setExpiresIn(300);
        expectedToken.setTokenType("Bearer");

        when(individualMapper.map(any())).thenReturn(new IndividualCreateProfileRequest());
        when(personServiceClient.createProfile(any())).thenReturn(Mono.just(individualInfo));
        when(client.createUser(anyString(), anyString(), anyMap())).thenReturn(Mono.empty());
        when(personServiceClient.activateProfile(any())).thenReturn(Mono.empty());
        when(tokenService.login(request.getEmail(), request.getPassword())).thenReturn(Mono.just(expectedToken));

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
        verify(personServiceClient).createProfile(any());
        verify(client).createUser(eq("test@test.com"), eq("password"), anyMap());
        verify(personServiceClient).activateProfile(individualInfo.getId());
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

        verifyNoInteractions(client, personServiceClient, tokenService);
    }

    @Test
    void shouldFailWhenUserAlreadyExists() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("existing@test.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        IndividualInfoResponse individualInfo = new IndividualInfoResponse();
        individualInfo.setId(UUID.randomUUID());
        individualInfo.setUserId(UUID.randomUUID());

        when(individualMapper.map(any())).thenReturn(new IndividualCreateProfileRequest());
        when(personServiceClient.createProfile(any())).thenReturn(Mono.just(individualInfo));
        when(client.createUser(anyString(), anyString(), anyMap()))
                .thenReturn(Mono.error(new KeycloakClientConflictException("User already exists")));
        when(personServiceClient.purgeProfile(any())).thenReturn(Mono.empty());

        // When
        StepVerifier.create(userService.register(request))
                // Then
                .expectError(KeycloakClientConflictException.class)
                .verify();

        verify(client).createUser(eq("existing@test.com"), eq("password123"), anyMap());
        verify(personServiceClient).purgeProfile(individualInfo.getId());
        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldFailWhenCreateUserThrowsError() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        IndividualInfoResponse individualInfo = new IndividualInfoResponse();
        individualInfo.setId(UUID.randomUUID());
        individualInfo.setUserId(UUID.randomUUID());

        when(individualMapper.map(any())).thenReturn(new IndividualCreateProfileRequest());
        when(personServiceClient.createProfile(any())).thenReturn(Mono.just(individualInfo));
        when(client.createUser(anyString(), anyString(), anyMap()))
                .thenReturn(Mono.error(new RuntimeException("Keycloak error")));

        // When
        StepVerifier.create(userService.register(request))
                .expectError(RuntimeException.class)
                .verify();

        verify(client).createUser(eq("test@test.com"), eq("password123"), anyMap());
        verify(personServiceClient, never()).purgeProfile(any());
        verifyNoInteractions(tokenService);
    }

    @Test
    void shouldFailWhenLoginFails() {
        // Given
        UserRegistrationRequest request = new UserRegistrationRequest();
        request.setEmail("test@test.com");
        request.setPassword("password123");
        request.setConfirmPassword("password123");

        IndividualInfoResponse individualInfo = new IndividualInfoResponse();
        individualInfo.setId(UUID.randomUUID());
        individualInfo.setUserId(UUID.randomUUID());

        when(individualMapper.map(any())).thenReturn(new IndividualCreateProfileRequest());
        when(personServiceClient.createProfile(any())).thenReturn(Mono.just(individualInfo));
        when(client.createUser(anyString(), anyString(), anyMap())).thenReturn(Mono.empty());
        when(personServiceClient.activateProfile(any())).thenReturn(Mono.empty());
        when(tokenService.login(anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Login failed")));

        // When
        StepVerifier.create(userService.register(request))
                .expectError(RuntimeException.class)
                .verify();

        verify(client).createUser(eq("test@test.com"), eq("password123"), anyMap());
        verify(personServiceClient).activateProfile(individualInfo.getId());
        verify(tokenService).login("test@test.com", "password123");
    }

}
