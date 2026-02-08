package com.ersted.aspect;

import com.ersted.annotation.Counted;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CountedAspect {

    private final MeterRegistry meterRegistry;


    @Around("@annotation(counted)")
    public Object count(ProceedingJoinPoint point, Counted counted) throws Throwable {
        String metricName = counted.value().isEmpty()
                ? point.getSignature().toShortString()
                : counted.value();

        Tags baseTags = Tags.of(counted.tags());

        Object result = point.proceed();

        if (result instanceof Mono<?> mono) {
            return countMono(mono, metricName, baseTags, counted.recordErrors());
        }

        if (result instanceof Flux<?> flux) {
            return countFlux(flux, metricName, baseTags, counted.recordErrors());
        }

        meterRegistry.counter(metricName, baseTags).increment();

        return result;
    }

    private <T> Mono<T> countMono(Mono<T> mono, String metricName, Tags baseTags, boolean recordErrors) {
        return mono
                .doOnSuccess(_ -> meterRegistry.counter(metricName, baseTags.and("status", "success")).increment())
                .doOnError(error -> {
                    if (recordErrors) {
                        meterRegistry.counter(metricName,
                                baseTags
                                        .and("status", "error")
                                        .and("exception", error.getClass().getSimpleName())
                        ).increment();
                    }
                });
    }

    private <T> Flux<T> countFlux(Flux<T> flux, String metricName, Tags baseTags, boolean recordErrors) {
        return flux
                .doOnComplete(() -> meterRegistry
                        .counter(metricName, baseTags.and("status", "success"))
                        .increment()
                )
                .doOnError(error -> {
                    if (recordErrors) {
                        meterRegistry.counter(metricName,
                                baseTags
                                        .and("status", "error")
                                        .and("exception", error.getClass().getSimpleName())
                        ).increment();
                    }
                });
    }

}
