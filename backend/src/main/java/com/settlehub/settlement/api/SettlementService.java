package com.settlehub.settlement.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.api.PageResponse;
import com.settlehub.common.exception.BusinessException;
import com.settlehub.ops.AuditLogService;
import com.settlehub.organization.domain.UserAccount;
import com.settlehub.organization.domain.UserAccountRepository;
import com.settlehub.organization.domain.UserRole;
import com.settlehub.settlement.domain.Settlement;
import com.settlehub.settlement.domain.SettlementRepository;
import com.settlehub.settlement.domain.SettlementStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final UserAccountRepository userAccountRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PageResponse<SettlementResponse> list(
            AuthUser actor,
            SettlementStatus status,
            Long agencyId,
            Long merchantId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            Pageable pageable
    ) {
        Specification<Settlement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            applyScope(actor, predicates, root, cb);
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (agencyId != null) {
                if (actor.role() == UserRole.AGENCY && !agencyId.equals(actor.agencyId())) {
                    throw BusinessException.forbidden("Cannot query other agency settlements");
                }
                predicates.add(cb.equal(root.get("agency").get("id"), agencyId));
            }
            if (merchantId != null) {
                if (actor.role() == UserRole.MERCHANT && !merchantId.equals(actor.merchantId())) {
                    throw BusinessException.forbidden("Cannot query other merchant settlements");
                }
                predicates.add(cb.equal(root.get("merchant").get("id"), merchantId));
            }
            if (periodStart != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("periodStart"), periodStart));
            }
            if (periodEnd != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("periodEnd"), periodEnd));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<SettlementResponse> page = settlementRepository.findAll(spec, pageable)
                .map(SettlementResponse::summary);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public SettlementResponse get(AuthUser actor, Long id) {
        Settlement settlement = settlementRepository.findDetailById(id)
                .orElseThrow(() -> BusinessException.notFound("Settlement not found"));
        assertCanRead(actor, settlement);
        return SettlementResponse.detail(settlement);
    }

    @Transactional
    public SettlementResponse hold(AuthUser actor, Long id, SettlementHoldRequest request) {
        assertAdmin(actor);
        Settlement settlement = settlementRepository.findDetailById(id)
                .orElseThrow(() -> BusinessException.notFound("Settlement not found"));
        String before = settlement.getStatus().name();
        settlement.hold(request.reason());

        UserAccount admin = userAccountRepository.findById(actor.id()).orElseThrow();
        auditLogService.record(
                admin,
                "SETTLEMENT_HOLD",
                "Settlement",
                settlement.getId(),
                before,
                settlement.getStatus().name(),
                request.reason()
        );
        return SettlementResponse.detail(settlement);
    }

    @Transactional
    public SettlementResponse confirm(AuthUser actor, Long id) {
        assertAdmin(actor);
        Settlement settlement = settlementRepository.findDetailById(id)
                .orElseThrow(() -> BusinessException.notFound("Settlement not found"));
        UserAccount admin = userAccountRepository.findById(actor.id()).orElseThrow();
        String before = settlement.getStatus().name();
        settlement.confirm(admin, LocalDateTime.now());

        auditLogService.record(
                admin,
                "SETTLEMENT_CONFIRM",
                "Settlement",
                settlement.getId(),
                before,
                settlement.getStatus().name(),
                null
        );
        return SettlementResponse.detail(settlement);
    }

    @Transactional
    public SettlementResponse readyForPayout(AuthUser actor, Long id) {
        assertAdmin(actor);
        Settlement settlement = settlementRepository.findDetailById(id)
                .orElseThrow(() -> BusinessException.notFound("Settlement not found"));
        String before = settlement.getStatus().name();
        settlement.markReadyForPayout();

        UserAccount admin = userAccountRepository.findById(actor.id()).orElseThrow();
        auditLogService.record(
                admin,
                "SETTLEMENT_READY_FOR_PAYOUT",
                "Settlement",
                settlement.getId(),
                before,
                settlement.getStatus().name(),
                null
        );
        return SettlementResponse.detail(settlement);
    }

    private void assertAdmin(AuthUser actor) {
        if (actor.role() != UserRole.ADMIN) {
            throw BusinessException.forbidden("Only ADMIN can change settlement status");
        }
    }

    private void assertCanRead(AuthUser actor, Settlement settlement) {
        switch (actor.role()) {
            case ADMIN -> {
            }
            case AGENCY -> {
                if (actor.agencyId() == null || !actor.agencyId().equals(settlement.getAgency().getId())) {
                    throw BusinessException.forbidden("Cannot access settlements outside your agency");
                }
            }
            case MERCHANT -> {
                if (actor.merchantId() == null || !actor.merchantId().equals(settlement.getMerchant().getId())) {
                    throw BusinessException.forbidden("Cannot access settlements outside your merchant");
                }
            }
        }
    }

    private void applyScope(
            AuthUser actor,
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<Settlement> root,
            jakarta.persistence.criteria.CriteriaBuilder cb
    ) {
        switch (actor.role()) {
            case ADMIN -> {
            }
            case AGENCY -> {
                if (actor.agencyId() == null) {
                    throw BusinessException.forbidden("Agency scope missing");
                }
                predicates.add(cb.equal(root.get("agency").get("id"), actor.agencyId()));
            }
            case MERCHANT -> {
                if (actor.merchantId() == null) {
                    throw BusinessException.forbidden("Merchant scope missing");
                }
                predicates.add(cb.equal(root.get("merchant").get("id"), actor.merchantId()));
            }
        }
    }
}
