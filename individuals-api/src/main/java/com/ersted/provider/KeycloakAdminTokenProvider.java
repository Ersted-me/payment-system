package com.ersted.provider;

import com.ersted.dto.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.function.Supplier;

@Slf4j
@Component
public class KeycloakAdminTokenProvider {

    private static final int DEFAULT_TOKEN_TTL_SECONDS = 300;
    private static final long TOKEN_REFRESH_MARGIN_SECONDS = 60;

    private volatile TokenResponse cachedToken;
    private volatile Instant expiresAt;


    public Mono<TokenResponse> getToken(Supplier<Mono<TokenResponse>> tokenSupplier) {
        if (isTokenValid()) {
            log.debug("Using cached admin token");
            return Mono.just(cachedToken);
        }

        log.debug("Cache miss, fetching new admin token");
        return tokenSupplier.get()
                .doOnSuccess(this::cacheToken);
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
                    DEFAULT_TOKEN_TTL_SECONDS);
            return DEFAULT_TOKEN_TTL_SECONDS;
        }
        return expiresIn;
    }

    private Instant calculateExpiration(int expiresInSeconds) {
        long safetyMargin = Math.min(TOKEN_REFRESH_MARGIN_SECONDS, expiresInSeconds / 2);
        return Instant.now().plusSeconds(expiresInSeconds - safetyMargin);
    }

}