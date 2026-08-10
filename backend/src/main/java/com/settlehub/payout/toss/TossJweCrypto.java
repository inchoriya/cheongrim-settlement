package com.settlehub.payout.toss;

import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 토스 지급대행 ENCRYPTION 모드용 JWE (dir + A256GCM).
 * @see <a href="https://docs.tosspayments.com/guides/v2/payouts">지급대행 가이드</a>
 */
@Component
public class TossJweCrypto {

    public String encrypt(String jsonPayload, String securityKeyHex) {
        try {
            byte[] key = HexFormat.of().parseHex(securityKeyHex);
            JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.DIR, EncryptionMethod.A256GCM)
                    .customParam("iat", OffsetDateTime.now(ZoneId.of("Asia/Seoul")).toString())
                    .customParam("nonce", UUID.randomUUID().toString())
                    .build();
            JWEObject jwe = new JWEObject(header, new Payload(jsonPayload));
            jwe.encrypt(new DirectEncrypter(key));
            return jwe.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt Toss request body", e);
        }
    }

    public String decrypt(String encryptedJwe, String securityKeyHex) {
        try {
            byte[] key = HexFormat.of().parseHex(securityKeyHex);
            JWEObject jwe = JWEObject.parse(encryptedJwe);
            jwe.decrypt(new DirectDecrypter(key));
            return jwe.getPayload().toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt Toss response body", e);
        }
    }
}
