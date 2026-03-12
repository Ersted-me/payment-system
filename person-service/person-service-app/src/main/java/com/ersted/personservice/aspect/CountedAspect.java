package com.ersted.personservice.aspect;

import com.ersted.personservice.annotation.Counted;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CountedAspect {

    private final MeterRegistry registry;

    @Around("@annotation(counted)")
    public Object count(ProceedingJoinPoint point, Counted counted) throws Throwable {

        String metricName = counted.value().isEmpty()
                ? point.getSignature().toShortString()
                : counted.value();

        Tags baseTags = Tags.of(counted.tags());

        try {
            Object result = point.proceed();
            registry.counter(metricName, baseTags.and("status", "success")).increment();
            return result;
        } catch (Throwable error) {
            if (counted.recordErrors()) {
                registry.counter(
                                metricName,
                                baseTags
                                        .and("status", "error")
                                        .and("exception", error.getClass().getSimpleName())
                        )
                        .increment();
            }
            throw error;
        }
    }

}
