package com.settlehub.auth.jwt;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.organization.domain.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(AuthUser user) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.expirationSeconds());

        return Jwts.builder()
                .subject(String.valueOf(user.id()))
                .claim("email", user.email())
                .claim("role", user.role().name())
                .claim("agencyId", user.agencyId())
                .claim("merchantId", user.merchantId())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    public AuthUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return AuthUser.fromToken(
                Long.valueOf(claims.getSubject()),
                claims.get("email", String.class),
                UserRole.valueOf(claims.get("role", String.class)),
                toLong(claims.get("agencyId")),
                toLong(claims.get("merchantId"))
        );
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public long expirationSeconds() {
        return properties.expirationSeconds();
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer i) {
            return i.longValue();
        }
        if (value instanceof Long l) {
            return l;
        }
        return Long.valueOf(value.toString());
    }
}
