package com.ersted.individualsapi.config;

import com.ersted.individualsapi.client.KeycloakClient;
import com.ersted.personservice.sdk.api.IndividualsApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import reactor.core.publisher.Mono;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.ImportHttpServices;

@Slf4j
@Configuration(proxyBeanMethods = false)
@ImportHttpServices(
        group = "person-service",
        basePackages = "com.ersted.personservice.sdk.api",
        types = { IndividualsApi.class },
        clientType = HttpServiceGroup.ClientType.WEB_CLIENT
)
public class SdkConfig {

    @Bean
    WebClientHttpServiceGroupConfigurer personServiceGroupConfigurer(Environment env, KeycloakClient keycloakClient) {
        String baseUrl = env.getProperty(
                "spring.http.serviceclient.person-service.base-url",
                "http://localhost:8080"
        );
        ExchangeFilterFunction bearerAuthFilter = (request, next) ->
                keycloakClient.adminToken()
                        .doOnSuccess(token -> log.info("Sending request to person-service: {} {}, token present: {}",
                                request.method(), request.url(), token != null && token.getAccessToken() != null))
                        .map(token -> ClientRequest.from(request)
                                .headers(h -> h.set(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken()))
                                .build())
                        .flatMap(next::exchange)
                        .flatMap(response -> {
                            if (response.statusCode() == HttpStatus.UNAUTHORIZED) {
                                keycloakClient.invalidateAdminToken();
                                return keycloakClient.adminToken()
                                        .map(token -> ClientRequest.from(request)
                                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getAccessToken())
                                                .build())
                                        .flatMap(next::exchange);
                            }
                            return Mono.just(response);
                        });

        return groups -> groups
                .filterByName("person-service")
                .forEachClient((group, builder) -> builder
                        .baseUrl(baseUrl)
                        .filter(bearerAuthFilter));
    }

}
