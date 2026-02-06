package com.ersted.client;

import java.time.Duration;

public record KeycloakClientSettings(
        ClientCredentials credentials,
        String tokenUrl,
        String userRegistrationUrl,
        RetrySettings retry
) {

    public record ClientCredentials(
            String clientId,
            String clientSecret
    ) {}

    public record RetrySettings(
            int attempts,
            Duration delay,
            Duration timeout
    ) {}

}
