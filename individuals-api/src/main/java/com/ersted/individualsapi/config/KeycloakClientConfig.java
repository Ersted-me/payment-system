package com.ersted.individualsapi.config;

import com.ersted.individualsapi.client.KeycloakClientSettings;
import com.ersted.individualsapi.provider.KeycloakAdminTokenProvider;
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
    public WebClient keycloakWebClient() {
        return WebClient.builder().build();
    }

    @Bean
    public KeycloakAdminTokenProvider keycloakAdminTokenProvider(){
        return new KeycloakAdminTokenProvider(
                properties.getAdminToken().getRefreshMarginSeconds(),
                properties.getAdminToken().getTtlSeconds()
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