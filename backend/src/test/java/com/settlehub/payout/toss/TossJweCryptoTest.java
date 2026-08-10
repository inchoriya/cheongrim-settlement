package com.settlehub.payout.toss;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TossJweCryptoTest {

    private final TossJweCrypto crypto = new TossJweCrypto();
    private static final String SECURITY_KEY =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void encryptAndDecryptRoundTrip() {
        String payload = "{\"hello\":\"world\",\"amount\":19501}";
        String encrypted = crypto.encrypt(payload, SECURITY_KEY);
        assertThat(encrypted).contains(".");
        String decrypted = crypto.decrypt(encrypted, SECURITY_KEY);
        assertThat(decrypted).isEqualTo(payload);
    }
}
