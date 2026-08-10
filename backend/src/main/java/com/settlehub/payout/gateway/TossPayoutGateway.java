package com.settlehub.payout.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.settlehub.organization.domain.MerchantRepository;
import com.settlehub.payout.config.PayoutProperties;
import com.settlehub.payout.domain.PgProvider;
import com.settlehub.payout.toss.TossPayoutClient;
import com.settlehub.payout.toss.TossPayoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "settlehub.payout.provider", havingValue = "toss")
public class TossPayoutGateway implements PayoutGateway {

    private static final String FAIL_ACCOUNT = "77701777777";
    private static final String FAIL_BANK = "295";

    private final TossPayoutClient tossPayoutClient;
    private final PayoutProperties payoutProperties;
    private final MerchantRepository merchantRepository;

    @Override
    @Transactional
    public PayoutResult requestPayout(PayoutRequest request) {
        try {
            String sellerId = ensureSeller(request);
            Map<String, Object> payout = buildPayoutBody(request, sellerId);
            JsonNode response = tossPayoutClient.requestPayouts(List.of(payout));
            return mapPayoutResponse(response);
        } catch (TossPayoutException e) {
            log.warn("Toss payout failed: {}", e.getMessage());
            return PayoutResult.failure(PgProvider.TOSS, e.getMessage(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected toss payout error", e);
            return PayoutResult.failure(PgProvider.TOSS, e.getMessage(), e.getMessage());
        }
    }

    private String ensureSeller(PayoutRequest request) {
        if (StringUtils.hasText(request.tossSellerId())) {
            return request.tossSellerId();
        }

        Map<String, Object> seller = new HashMap<>();
        seller.put("refSellerId", request.refSellerId());
        seller.put("businessType", "INDIVIDUAL_BUSINESS");
        seller.put("company", Map.of(
                "name", request.merchantName(),
                "representativeName", request.accountHolder(),
                "businessRegistrationNumber", "1234567890",
                "email", "merchant+" + request.beneficiaryId() + "@cheongrim.local",
                "phone", "01012345678"
        ));

        String bankCode = request.forceFail() ? FAIL_BANK : request.bankCode();
        String accountNumber = request.forceFail() ? FAIL_ACCOUNT : request.accountNumber();
        seller.put("account", Map.of(
                "bankCode", bankCode,
                "accountNumber", accountNumber,
                "holderName", request.accountHolder()
        ));
        seller.put("metadata", Map.of(
                "merchantId", String.valueOf(request.beneficiaryId()),
                "settlementId", String.valueOf(request.settlementId())
        ));

        JsonNode registered = tossPayoutClient.registerSeller(seller);
        String sellerId = extractSellerId(registered);
        if (!StringUtils.hasText(sellerId)) {
            throw new TossPayoutException("Seller id missing in Toss response: " + registered);
        }

        merchantRepository.findById(request.beneficiaryId()).ifPresent(merchant -> {
            merchant.assignTossSellerId(sellerId);
            merchantRepository.save(merchant);
        });
        return sellerId;
    }

    private Map<String, Object> buildPayoutBody(PayoutRequest request, String sellerId) {
        String scheduleType = payoutProperties.getToss().getScheduleType();
        Map<String, Object> body = new HashMap<>();
        body.put("destination", sellerId);
        body.put("scheduleType", scheduleType);
        body.put("amount", Map.of("currency", "KRW", "value", request.amount()));
        body.put("metadata", Map.of("settlementId", String.valueOf(request.settlementId())));
        if ("SCHEDULED".equalsIgnoreCase(scheduleType)) {
            body.put("payoutDate", resolvePayoutDate());
        }
        return body;
    }

    private String resolvePayoutDate() {
        String configured = payoutProperties.getToss().getPayoutDate();
        if (StringUtils.hasText(configured)) {
            return configured;
        }
        // 영업일 보정 없이 익일 (샌드박스/데모용). 실서비스는 영업일 캘린더 필요.
        return LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private PayoutResult mapPayoutResponse(JsonNode response) {
        JsonNode entity = response.path("entityBody");
        if (entity.isMissingNode() || entity.isNull()) {
            entity = response.isArray() && !response.isEmpty() ? response.get(0) : response;
        }
        if (entity.isArray() && !entity.isEmpty()) {
            entity = entity.get(0);
        }

        String status = text(entity, "status");
        String payoutId = text(entity, "id");
        if (!StringUtils.hasText(payoutId)) {
            payoutId = text(entity.path("entityBody"), "id");
        }

        String raw = response.toString();
        if ("FAILED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status) || "CANCELED".equalsIgnoreCase(status)) {
            String reason = text(entity.path("error"), "message");
            if (!StringUtils.hasText(reason)) {
                reason = status;
            }
            return PayoutResult.failure(PgProvider.TOSS, reason, raw);
        }

        if (!StringUtils.hasText(payoutId)) {
            return PayoutResult.failure(PgProvider.TOSS, "Missing payout id", raw);
        }
        return PayoutResult.success(PgProvider.TOSS, payoutId, raw);
    }

    private String extractSellerId(JsonNode response) {
        JsonNode entity = response.path("entityBody");
        if (!entity.isMissingNode() && !entity.isNull()) {
            return text(entity, "id");
        }
        return text(response, "id");
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }
}
