package com.settlehub.payout.gateway;

import com.settlehub.payout.domain.PgProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "settlehub.payout.provider", havingValue = "mock", matchIfMissing = true)
public class MockPayoutGateway implements PayoutGateway {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Override
    public PayoutResult requestPayout(PayoutRequest request) {
        if (request.forceFail()) {
            String raw = """
                    {"provider":"MOCK","success":false,"settlementId":%d,"amount":%d,"reason":"FORCE_FAIL"}
                    """.formatted(request.settlementId(), request.amount()).trim();
            return PayoutResult.failure(PgProvider.MOCK, "FORCE_FAIL", raw);
        }

        String txId = "MOCK-" + LocalDateTime.now().format(DAY) + "-"
                + UUID.randomUUID().toString().substring(0, 8);
        String raw = """
                {"provider":"MOCK","success":true,"settlementId":%d,"amount":%d,"pgTransactionId":"%s"}
                """.formatted(request.settlementId(), request.amount(), txId).trim();
        return PayoutResult.success(PgProvider.MOCK, txId, raw);
    }
}
