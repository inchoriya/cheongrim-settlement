package com.settlehub.payout.config;

import com.settlehub.payout.domain.PgProvider;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "settlehub.payout")
public class PayoutProperties {

    /**
     * mock | toss
     */
    private String provider = "mock";

    private final Toss toss = new Toss();

    public PgProvider resolvedProvider() {
        return "toss".equalsIgnoreCase(provider) ? PgProvider.TOSS : PgProvider.MOCK;
    }

    public boolean isToss() {
        return resolvedProvider() == PgProvider.TOSS;
    }

    @Getter
    @Setter
    public static class Toss {
        private String secretKey = "";
        private String securityKey = "";
        private String baseUrl = "https://api.tosspayments.com";
        private String scheduleType = "SCHEDULED";
        /** yyyy-MM-dd, empty면 요청 시 익일(단순 계산) */
        private String payoutDate = "";
        private int connectTimeoutMs = 5_000;
        private int readTimeoutMs = 15_000;
    }
}
