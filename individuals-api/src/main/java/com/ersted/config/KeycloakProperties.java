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
@Builder
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

    private AdminTokenProperties adminToken;

    private RequestsRetryProperties requestsRetry;

    @Getter
    @Setter
    @Builder
    public static class AdminTokenProperties {

        @Min(value = 30)
        private int ttlSeconds;

        @Min(value = 15)
        private long refreshMarginSeconds;

    }

    @Getter
    @Setter
    @Builder
    public static class RequestsRetryProperties {

        @Min(value = 0)
        private int attempts;

        @Min(value = 0)
        private int delaySeconds;

        @Min(value = 0)
        private int requestTimeoutSeconds;

    }


    public String getTokenUri() {
        return url + "/realms/" + realm + "/protocol/openid-connect/token";
    }

    public String getAdminUsersUri() {
        return url + "/admin/realms/" + realm + "/users";
    }
}

