package com.ersted.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {

    @NotBlank
    private String url;

    @NotBlank
    private String realm;

    @NotBlank
    private String clientId;

    @NotBlank
    private String clientSecret;

    private AdminTokenProperties adminToken = new AdminTokenProperties();

    private RequestsRetryProperties requestsRetry = new RequestsRetryProperties();

    @Getter
    @Setter
    public static class AdminTokenProperties {

        @Min(value = 30)
        private int ttlSeconds;

        @Min(value = 15)
        private long refreshMarginSeconds;

    }

    @Getter
    @Setter
    public static class RequestsRetryProperties {

        @Min(value = 0)
        private int attempts;

        @Min(value = 0)
        private int delaySeconds;

        @Min(value = 0)
        private int requestTimeoutSeconds;

    }

}

