package com.settlehub.payout.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.exception.BusinessException;
import com.settlehub.ops.AuditLogService;
import com.settlehub.organization.domain.Merchant;
import com.settlehub.organization.domain.UserAccount;
import com.settlehub.organization.domain.UserAccountRepository;
import com.settlehub.organization.domain.UserRole;
import com.settlehub.payout.config.PayoutProperties;
import com.settlehub.payout.domain.PayoutStatus;
import com.settlehub.payout.domain.PayoutTransaction;
import com.settlehub.payout.domain.PayoutTransactionRepository;
import com.settlehub.payout.gateway.PayoutGateway;
import com.settlehub.payout.gateway.PayoutRequest;
import com.settlehub.payout.gateway.PayoutResult;
import com.settlehub.settlement.domain.Settlement;
import com.settlehub.settlement.domain.SettlementRepository;
import com.settlehub.settlement.domain.SettlementStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayoutService {

    private final PayoutGateway payoutGateway;
    private final PayoutProperties payoutProperties;
    private final PayoutTransactionRepository payoutTransactionRepository;
    private final SettlementRepository settlementRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public PayoutResponse requestPayout(AuthUser actor, PayoutCreateRequest request) {
        assertAdmin(actor);

        Settlement settlement = settlementRepository.findById(request.settlementId())
                .orElseThrow(() -> BusinessException.notFound("Settlement not found"));

        settlement.assertPayable();

        if (settlement.getStatus() == SettlementStatus.PAID
                || payoutTransactionRepository.existsBySettlementIdAndStatus(
                settlement.getId(), PayoutStatus.SUCCEEDED)) {
            throw BusinessException.invalidState("Settlement already paid");
        }

        boolean forceFail = Boolean.TRUE.equals(request.forceFail());
        long amount = settlement.getTotalMerchantSettlementAmount();
        LocalDateTime now = LocalDateTime.now();
        Merchant merchant = settlement.getMerchant();

        PayoutTransaction tx = payoutTransactionRepository.save(PayoutTransaction.request(
                settlement,
                amount,
                merchant.getId(),
                payoutProperties.resolvedProvider(),
                now
        ));

        PayoutResult result = payoutGateway.requestPayout(new PayoutRequest(
                settlement.getId(),
                amount,
                merchant.getId(),
                merchant.refSellerId(),
                merchant.getName(),
                defaultBankCode(merchant),
                defaultAccountNumber(merchant),
                defaultAccountHolder(merchant),
                merchant.getTossSellerId(),
                forceFail
        ));

        String before = settlement.getStatus().name();
        if (result.success()) {
            tx.succeed(result.pgTransactionId(), result.rawResponse(), LocalDateTime.now());
            settlement.markPaid();
        } else {
            tx.fail(result.failureReason(), result.rawResponse(), LocalDateTime.now());
            settlement.markPayoutFailed();
        }

        UserAccount admin = userAccountRepository.findById(actor.id()).orElseThrow();
        auditLogService.record(
                admin,
                result.success() ? "PAYOUT_SUCCEEDED" : "PAYOUT_FAILED",
                "Settlement",
                settlement.getId(),
                before,
                settlement.getStatus().name(),
                result.failureReason()
        );

        return PayoutResponse.from(tx);
    }

    @Transactional(readOnly = true)
    public List<PayoutResponse> list(AuthUser actor, Long settlementId, PayoutStatus status) {
        assertAdmin(actor);
        Specification<PayoutTransaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (settlementId != null) {
                predicates.add(cb.equal(root.get("settlement").get("id"), settlementId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return payoutTransactionRepository.findAll(spec).stream()
                .map(PayoutResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PayoutResponse get(AuthUser actor, Long id) {
        assertAdmin(actor);
        PayoutTransaction tx = payoutTransactionRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Payout not found"));
        return PayoutResponse.from(tx);
    }

    private void assertAdmin(AuthUser actor) {
        if (actor.role() != UserRole.ADMIN) {
            throw BusinessException.forbidden("Only ADMIN can manage payouts");
        }
    }

    private static String defaultBankCode(Merchant merchant) {
        return StringUtils.hasText(merchant.getBankCode()) ? merchant.getBankCode() : "088";
    }

    private static String defaultAccountNumber(Merchant merchant) {
        return StringUtils.hasText(merchant.getAccountNumber()) ? merchant.getAccountNumber() : "110123456789";
    }

    private static String defaultAccountHolder(Merchant merchant) {
        return StringUtils.hasText(merchant.getAccountHolder()) ? merchant.getAccountHolder() : merchant.getName();
    }
}
