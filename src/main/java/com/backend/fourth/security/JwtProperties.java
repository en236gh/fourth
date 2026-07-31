package com.backend.fourth.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret = "dev-secret-key-change-me-in-production";
    private long accessTokenExpirationMs = 300_000L;
    private long refreshTokenExpirationMs = 7L * 24 * 60 * 60 * 1000;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public void setAccessTokenExpirationMs(long accessTokenExpirationMs) {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public void setRefreshTokenExpirationMs(long refreshTokenExpirationMs) {
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }
}
