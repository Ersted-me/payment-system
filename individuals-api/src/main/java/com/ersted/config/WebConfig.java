package com.ersted.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Configuration
public class WebConfig {

    @Bean
    public WebClient keycloakWebClient(WebClient.Builder builder) {
        return builder
                .filter((request, next) -> {
                    log.debug("Keycloak request: {} {}", request.method(), request.url());
                    return next.exchange(request);
                })
                .build();
    }

}
