package com.settlehub.payout.toss;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settlehub.payout.config.PayoutProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "settlehub.payout.provider", havingValue = "toss")
public class TossPayoutClient {

    private final PayoutProperties payoutProperties;
    private final TossJweCrypto jweCrypto;
    private final ObjectMapper objectMapper;

    public JsonNode registerSeller(Map<String, Object> sellerBody) {
        return postEncrypted("/v2/sellers", sellerBody);
    }

    public JsonNode requestPayouts(List<Map<String, Object>> payoutBodies) {
        return postEncrypted("/v2/payouts", payoutBodies);
    }

    private JsonNode postEncrypted(String path, Object body) {
        PayoutProperties.Toss toss = payoutProperties.getToss();
        try {
            String json = objectMapper.writeValueAsString(body);
            String encrypted = jweCrypto.encrypt(json, toss.getSecurityKey());

            RestClient client = restClient();
            String responseBody = client.post()
                    .uri(path)
                    .contentType(MediaType.TEXT_PLAIN)
                    .header("Authorization", basicAuth(toss.getSecretKey()))
                    .header("TossPayments-api-security-mode", "ENCRYPTION")
                    .header("Idempotency-Key", UUID.randomUUID().toString())
                    .body(encrypted)
                    .retrieve()
                    .body(String.class);

            String decrypted = jweCrypto.decrypt(responseBody == null ? "" : responseBody.trim(), toss.getSecurityKey());
            return objectMapper.readTree(decrypted);
        } catch (RestClientResponseException e) {
            String raw = e.getResponseBodyAsString();
            String detail = raw;
            try {
                if (raw != null && !raw.isBlank() && raw.contains(".")) {
                    detail = jweCrypto.decrypt(raw.trim(), toss.getSecurityKey());
                }
            } catch (Exception ignored) {
                // keep raw
            }
            throw new TossPayoutException("Toss API error HTTP " + e.getStatusCode().value() + ": " + detail, e);
        } catch (TossPayoutException e) {
            throw e;
        } catch (Exception e) {
            throw new TossPayoutException("Toss API call failed: " + e.getMessage(), e);
        }
    }

    private RestClient restClient() {
        PayoutProperties.Toss toss = payoutProperties.getToss();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(toss.getConnectTimeoutMs());
        factory.setReadTimeout(toss.getReadTimeoutMs());
        return RestClient.builder()
                .baseUrl(toss.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    private static String basicAuth(String secretKey) {
        String token = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }
}
