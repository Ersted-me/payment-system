package com.ersted.individualsapi.config;

import com.ersted.personservice.sdk.api.IndividualsApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupConfigurer;
import org.springframework.web.service.registry.HttpServiceGroup;
import org.springframework.web.service.registry.ImportHttpServices;

@Configuration(proxyBeanMethods = false)
@ImportHttpServices(
        group = "person-service",
        basePackages = "com.ersted.personservice.sdk.api",
        types = { IndividualsApi.class },
        clientType = HttpServiceGroup.ClientType.WEB_CLIENT
)
public class SdkConfig {

    @Bean
    WebClientHttpServiceGroupConfigurer personServiceGroupConfigurer(Environment env) {
        String baseUrl = env.getProperty(
                "spring.http.serviceclient.person-service.base-url",
                "http://localhost:8080"
        );
        return groups -> groups
                .filterByName("person-service")
                .forEachClient((group, builder) -> builder.baseUrl(baseUrl));
    }

}
