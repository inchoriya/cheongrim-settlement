package com.settlehub.dashboard.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.exception.BusinessException;
import com.settlehub.order.domain.DeliveryOrder;
import com.settlehub.order.domain.DeliveryOrderRepository;
import com.settlehub.settlement.domain.Settlement;
import com.settlehub.settlement.domain.SettlementRepository;
import com.settlehub.settlement.domain.SettlementStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final EnumSet<SettlementStatus> PENDING_STATUSES = EnumSet.of(
            SettlementStatus.CALCULATED,
            SettlementStatus.CONFIRMED
    );

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final SettlementRepository settlementRepository;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(AuthUser actor, LocalDateTime from, LocalDateTime to) {
        Specification<DeliveryOrder> orderSpec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            applyOrderScope(actor, predicates, root, cb);
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("orderedAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        List<DeliveryOrder> orders = deliveryOrderRepository.findAll(orderSpec);
        long orderCount = orders.size();
        long orderAmountSum = orders.stream().mapToLong(DeliveryOrder::getOrderAmount).sum();

        Specification<Settlement> settlementSpec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            applySettlementScope(actor, predicates, root, cb);
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("periodStart"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("periodEnd"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        List<Settlement> settlements = settlementRepository.findAll(settlementSpec);
        long settlementPendingCount = settlements.stream()
                .filter(s -> PENDING_STATUSES.contains(s.getStatus()))
                .count();
        long heldCount = settlements.stream()
                .filter(s -> s.getStatus() == SettlementStatus.HELD)
                .count();
        long readyForPayoutCount = settlements.stream()
                .filter(s -> s.getStatus() == SettlementStatus.READY_FOR_PAYOUT)
                .count();
        long paidAmountSum = settlements.stream()
                .filter(s -> s.getStatus() == SettlementStatus.PAID)
                .mapToLong(Settlement::getTotalMerchantSettlementAmount)
                .sum();

        return new DashboardSummaryResponse(
                orderCount,
                orderAmountSum,
                settlementPendingCount,
                heldCount,
                readyForPayoutCount,
                paidAmountSum
        );
    }

    private void applyOrderScope(
            AuthUser actor,
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<DeliveryOrder> root,
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

    private void applySettlementScope(
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
