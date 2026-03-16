package com.ersted.individualsapi.config;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class Resilience4jConfig {

    @Bean
    CircuitBreakerRegistry circuitBreakerRegistry(
            @Value("${resilience4j.circuitbreaker.sliding-window-size:10}") int slidingWindowSize,
            @Value("${resilience4j.circuitbreaker.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${resilience4j.circuitbreaker.slow-call-rate-threshold:80}") float slowCallRateThreshold,
            @Value("${resilience4j.circuitbreaker.slow-call-duration-threshold-seconds:3}") long slowCallDurationThresholdSeconds,
            @Value("${resilience4j.circuitbreaker.wait-duration-in-open-state-seconds:10}") long waitDurationInOpenStateSeconds,
            @Value("${resilience4j.circuitbreaker.permitted-calls-in-half-open-state:3}") int permittedCallsInHalfOpenState
    ) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(slidingWindowSize)
                .failureRateThreshold(failureRateThreshold)
                .slowCallRateThreshold(slowCallRateThreshold)
                .slowCallDurationThreshold(Duration.ofSeconds(slowCallDurationThresholdSeconds))
                .waitDurationInOpenState(Duration.ofSeconds(waitDurationInOpenStateSeconds))
                .permittedNumberOfCallsInHalfOpenState(permittedCallsInHalfOpenState)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    BulkheadRegistry bulkheadRegistry(
            @Value("${resilience4j.bulkhead.max-concurrent-calls:20}") int maxConcurrentCalls,
            @Value("${resilience4j.bulkhead.max-wait-duration-ms:0}") long maxWaitDurationMs
    ) {
        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(maxConcurrentCalls)
                .maxWaitDuration(Duration.ofMillis(maxWaitDurationMs))
                .build();

        return BulkheadRegistry.of(config);
    }

}
