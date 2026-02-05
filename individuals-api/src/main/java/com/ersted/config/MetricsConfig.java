package com.ersted.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class MetricsConfig {

    @Value("${spring.application.name}")
    private String METRIC_PREFIX;


    @Bean
    public Counter userRegisterSuccessful(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + ".users.register.successful")
                .description("Total number of users successfully registered")
                .register(registry);
    }

    @Bean
    public Counter userRegisterFailed(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + ".users.register.failed")
                .description("Total number of users failed registered")
                .register(registry);
    }

    @Bean
    public Counter userLoginSuccessful(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + ".users.login.successful")
                .description("Total number of users successfully login")
                .register(registry);
    }

    @Bean
    public Counter userLoginFailed(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + ".users.login.failed")
                .description("Total number of users failed login")
                .register(registry);
    }

    @Bean
    public Counter userGetInfoSuccessful(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + ".users.info.successful")
                .description("Total number of users successfully get info")
                .register(registry);
    }

    @Bean
    public Counter userGetInfoFailed(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + ".users.info.failed")
                .description("Total number of users failed get info")
                .register(registry);
    }

    @Bean
    public Counter userRefreshTokenSuccessful(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + ".users.refresh.token.successful")
                .description("Total number of users successfully refresh token")
                .register(registry);
    }

    @Bean
    public Counter userRefreshTokenFailed(MeterRegistry registry) {
        return Counter.builder(METRIC_PREFIX + ".users.refresh.token.failed")
                .description("Total number of users failed refresh token")
                .register(registry);
    }

}

