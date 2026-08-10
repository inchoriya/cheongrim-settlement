package com.settlehub.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "settlehub.jwt")
public record JwtProperties(
        String secret,
        long expirationSeconds
) {
}
