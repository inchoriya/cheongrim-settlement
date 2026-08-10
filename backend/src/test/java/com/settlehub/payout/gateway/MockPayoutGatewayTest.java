package com.settlehub.payout.gateway;

import com.settlehub.payout.domain.PgProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockPayoutGatewayTest {

    private final MockPayoutGateway gateway = new MockPayoutGateway();

    @Test
    void successReturnsTransactionId() {
        PayoutResult result = gateway.requestPayout(sample(false));
        assertThat(result.success()).isTrue();
        assertThat(result.provider()).isEqualTo(PgProvider.MOCK);
        assertThat(result.pgTransactionId()).startsWith("MOCK-");
        assertThat(result.rawResponse()).contains("\"success\":true");
    }

    @Test
    void forceFailReturnsReason() {
        PayoutResult result = gateway.requestPayout(sample(true));
        assertThat(result.success()).isFalse();
        assertThat(result.failureReason()).isEqualTo("FORCE_FAIL");
        assertThat(result.pgTransactionId()).isNull();
    }

    private static PayoutRequest sample(boolean forceFail) {
        return new PayoutRequest(
                1L, 19_501L, 10L, "MCHM001", "김밥천국",
                "088", "110123456789", "김밥천국", null, forceFail
        );
    }
}
