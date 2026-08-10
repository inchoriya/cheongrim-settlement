package com.settlehub.auth.jwt;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.organization.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(new JwtProperties(
                "settlehub-test-secret-key-must-be-long-enough-123456",
                3600
        ));
    }

    @Test
    void createAndParseToken() {
        AuthUser user = AuthUser.fromToken(1L, "admin@cheongrim.local", UserRole.ADMIN, null, null);

        String token = provider.createAccessToken(user);
        AuthUser parsed = provider.parseToken(token);

        assertThat(parsed.id()).isEqualTo(1L);
        assertThat(parsed.email()).isEqualTo("admin@cheongrim.local");
        assertThat(parsed.role()).isEqualTo(UserRole.ADMIN);
        assertThat(provider.isValid(token)).isTrue();
        assertThat(provider.isValid("invalid.token.value")).isFalse();
    }
}
