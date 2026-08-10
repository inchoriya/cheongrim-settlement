package com.settlehub.payout.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.settlehub.organization.domain.MerchantRepository;
import com.settlehub.payout.config.PayoutProperties;
import com.settlehub.payout.domain.PgProvider;
import com.settlehub.payout.toss.TossJweCrypto;
import com.settlehub.payout.toss.TossPayoutClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TossPayoutGatewayTest {

    private static final String SECURITY_KEY =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private MockWebServer server;
    private TossJweCrypto crypto;
    private TossPayoutGateway gateway;

    @Mock
    private MerchantRepository merchantRepository;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        crypto = new TossJweCrypto();

        PayoutProperties properties = new PayoutProperties();
        properties.setProvider("toss");
        properties.getToss().setSecretKey("test_sk_demo");
        properties.getToss().setSecurityKey(SECURITY_KEY);
        properties.getToss().setBaseUrl(server.url("/").toString().replaceAll("/$", ""));
        properties.getToss().setScheduleType("SCHEDULED");
        properties.getToss().setPayoutDate("2026-08-10");

        TossPayoutClient client = new TossPayoutClient(properties, crypto, new ObjectMapper());
        gateway = new TossPayoutGateway(client, properties, merchantRepository);
        when(merchantRepository.findById(anyLong())).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void registersSellerThenRequestsPayout() throws Exception {
        String sellerResponse = """
                {"version":"2024-06-01","entityType":"seller","entityBody":{"id":"seller_demo_1","refSellerId":"MCHM001"}}
                """;
        String payoutResponse = """
                {"version":"2024-06-01","entityType":"payout","entityBody":{"id":"payout_demo_1","status":"REQUESTED","destination":"seller_demo_1"}}
                """;
        server.enqueue(new MockResponse()
                .setBody(crypto.encrypt(sellerResponse, SECURITY_KEY))
                .addHeader("Content-Type", "text/plain"));
        server.enqueue(new MockResponse()
                .setBody(crypto.encrypt(payoutResponse, SECURITY_KEY))
                .addHeader("Content-Type", "text/plain"));

        PayoutResult result = gateway.requestPayout(new PayoutRequest(
                7L, 19_501L, 1L, "MCHM001", "김밥천국",
                "088", "110123456789", "김밥천국", null, false
        ));

        assertThat(result.success()).isTrue();
        assertThat(result.provider()).isEqualTo(PgProvider.TOSS);
        assertThat(result.pgTransactionId()).isEqualTo("payout_demo_1");

        RecordedRequest sellerReq = server.takeRequest(1, TimeUnit.SECONDS);
        RecordedRequest payoutReq = server.takeRequest(1, TimeUnit.SECONDS);
        assertThat(sellerReq.getPath()).isEqualTo("/v2/sellers");
        assertThat(sellerReq.getHeader("TossPayments-api-security-mode")).isEqualTo("ENCRYPTION");
        assertThat(sellerReq.getHeader("Authorization")).startsWith("Basic ");
        assertThat(payoutReq.getPath()).isEqualTo("/v2/payouts");
    }

    @Test
    void mapsFailedPayoutStatus() throws Exception {
        String sellerResponse = """
                {"entityBody":{"id":"seller_demo_2"}}
                """;
        String payoutResponse = """
                {"entityBody":{"id":"payout_fail_1","status":"FAILED","error":{"message":"BANK_ERROR"}}}
                """;
        server.enqueue(new MockResponse().setBody(crypto.encrypt(sellerResponse, SECURITY_KEY)));
        server.enqueue(new MockResponse().setBody(crypto.encrypt(payoutResponse, SECURITY_KEY)));

        PayoutResult result = gateway.requestPayout(new PayoutRequest(
                8L, 10_000L, 2L, "MCHM002", "치킨하우스",
                "004", "123456789012", "치킨하우스", null, false
        ));

        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo("BANK_ERROR");
        assertThat(result.provider()).isEqualTo(PgProvider.TOSS);
    }
}
