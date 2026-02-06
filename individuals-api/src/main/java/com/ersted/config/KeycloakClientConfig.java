package com.ersted.config;

import com.ersted.client.KeycloakClient;
import com.ersted.client.KeycloakClientSettings;
import com.ersted.exception.handler.KeycloakErrorHandler;
import com.ersted.provider.KeycloakAdminTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;


@Configuration
@RequiredArgsConstructor
public class KeycloakClientConfig {

    private final KeycloakProperties properties;


    @Bean
    public KeycloakClientSettings keycloakClientSettings() {
        return new KeycloakClientSettings(
                new KeycloakClientSettings.ClientCredentials(
                        properties.getClientId(),
                        properties.getClientSecret()
                ),
                buildTokenUrl(),
                buildUserRegistrationUrl(),
                new KeycloakClientSettings.RetrySettings(
                        properties.getRequestsRetry().getAttempts(),
                        Duration.ofSeconds(properties.getRequestsRetry().getDelaySeconds()),
                        Duration.ofSeconds(properties.getRequestsRetry().getRequestTimeoutSeconds())
                )
        );
    }

    @Bean
    public WebClient keycloakWebClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    public KeycloakAdminTokenProvider keycloakAdminTokenProvider(){
        return new KeycloakAdminTokenProvider(
                properties.getAdminToken().getRefreshMarginSeconds(),
                properties.getAdminToken().getTtlSeconds()
        );
    }

    @Bean
    public KeycloakClient keycloakClient(
            WebClient keycloakWebClient,
            KeycloakClientSettings settings,
            KeycloakErrorHandler errorHandler,
            KeycloakAdminTokenProvider keycloakAdminTokenProvider
    ) {
        return new KeycloakClient(
                keycloakWebClient,
                settings,
                errorHandler,
                keycloakAdminTokenProvider
        );
    }

    private String buildUserRegistrationUrl() {
        return UriComponentsBuilder.fromUriString(properties.getUrl())
                .pathSegment("admin", "realms", properties.getRealm(), "users")
                .build()
                .toUriString();
    }

    private String buildTokenUrl() {
        return UriComponentsBuilder.fromUriString(properties.getUrl())
                .pathSegment("realms", properties.getRealm(), "protocol", "openid-connect", "token")
                .build()
                .toUriString();
    }

}