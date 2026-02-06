package com.ersted.provider;

import com.ersted.dto.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@Slf4j
public class KeycloakAdminTokenProvider {

    private final AtomicReference<Mono<TokenResponse>> cachedTokenMono = new AtomicReference<>();

    private final long tokenRefreshMarginSeconds;
    private final int defaultTokenTtlSeconds;

    private volatile TokenResponse cachedToken;
    private volatile Instant expiresAt;

    public KeycloakAdminTokenProvider(long tokenRefreshMarginSeconds, int defaultTokenTtlSeconds) {
        this.tokenRefreshMarginSeconds = tokenRefreshMarginSeconds;
        this.defaultTokenTtlSeconds = defaultTokenTtlSeconds;
    }

    public Mono<TokenResponse> getToken(Supplier<Mono<TokenResponse>> tokenSupplier) {
        return Mono.defer(() -> {
            if (isTokenValid()) {
                return Mono.just(cachedToken);
            }
            return cachedTokenMono.updateAndGet(current -> {
                if (current != null) return current;
                return tokenSupplier.get()
                        .doOnSuccess(this::cacheToken)
                        .doFinally(_ -> cachedTokenMono.set(null))
                        .cache();
            });
        });
    }

    public boolean isTokenValid() {
        return cachedToken != null
                && expiresAt != null
                && Instant.now().isBefore(expiresAt);
    }

    public void invalidate() {
        log.info("Admin token cache invalidated");
        this.cachedToken = null;
        this.expiresAt = null;
    }

    private void cacheToken(TokenResponse token) {
        this.cachedToken = token;

        int expiresIn = getExpiresInSeconds(token);
        this.expiresAt = calculateExpiration(expiresIn);

        log.info("Admin token cached for {} seconds, expires at: {}", expiresIn, expiresAt);
    }

    private int getExpiresInSeconds(TokenResponse token) {
        Integer expiresIn = token.getExpiresIn();
        if (expiresIn == null || expiresIn <= 0) {
            log.warn("Token expiresIn is null or invalid, using default: {} seconds",
                    defaultTokenTtlSeconds);
            return defaultTokenTtlSeconds;
        }
        return expiresIn;
    }

    private Instant calculateExpiration(int expiresInSeconds) {
        long safetyMargin = Math.min(tokenRefreshMarginSeconds, expiresInSeconds / 2);
        return Instant.now().plusSeconds(expiresInSeconds - safetyMargin);
    }

}