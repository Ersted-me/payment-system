package com.ersted.config;

import com.ersted.client.KeycloakClient;
import com.ersted.exception.hendler.KeycloakErrorHandler;
import com.ersted.provider.KeycloakAdminTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Slf4j
@Configuration
public class KeycloakClientConfig {

    @Bean
    public KeycloakClient keycloakClient(
            WebClient keycloakWebClient,
            KeycloakErrorHandler errorHandler,
            KeycloakAdminTokenProvider adminTokenProvider,
            KeycloakProperties properties) {

        return new KeycloakClient(
                properties.getRequestsRetry().getAttempts(),
                Duration.ofSeconds(properties.getRequestsRetry().getDelaySeconds()),
                Duration.ofSeconds(properties.getRequestsRetry().getRequestTimeoutSeconds()),
                properties.getClientId(),
                properties.getClientSecret(),
                properties.getAdminUsersUri(),
                properties.getTokenUri(),
                keycloakWebClient,
                errorHandler,
                adminTokenProvider
        );
    }

    @Bean
    public KeycloakAdminTokenProvider keycloakAdminTokenProvider(KeycloakProperties properties){
        return new KeycloakAdminTokenProvider(
                properties.getAdminToken().getTtlSeconds(),
                properties.getAdminToken().getRefreshMarginSeconds()
        );
    }

}
