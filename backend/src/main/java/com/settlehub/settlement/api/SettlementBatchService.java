package com.settlehub.settlement.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.exception.BusinessException;
import com.settlehub.ops.domain.BatchJobLog;
import com.settlehub.ops.domain.BatchJobLogRepository;
import com.settlehub.ops.domain.BatchJobStatus;
import com.settlehub.organization.domain.Agency;
import com.settlehub.organization.domain.AgencyRepository;
import com.settlehub.organization.domain.Merchant;
import com.settlehub.organization.domain.UserAccount;
import com.settlehub.organization.domain.UserAccountRepository;
import com.settlehub.organization.domain.UserRole;
import com.settlehub.order.domain.DeliveryOrder;
import com.settlehub.order.domain.DeliveryOrderRepository;
import com.settlehub.order.domain.OrderStatus;
import com.settlehub.policy.PolicyResolver;
import com.settlehub.policy.domain.FeePolicy;
import com.settlehub.settlement.domain.AnomalyFlag;
import com.settlehub.settlement.domain.Settlement;
import com.settlehub.settlement.domain.SettlementBreakdown;
import com.settlehub.settlement.domain.SettlementCalculateCommand;
import com.settlehub.settlement.domain.SettlementCalculator;
import com.settlehub.settlement.domain.SettlementLine;
import com.settlehub.settlement.domain.SettlementRepository;
import com.settlehub.settlement.domain.SettlementStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementBatchService {

    private static final EnumSet<SettlementStatus> FINALIZED_STATUSES = EnumSet.of(
            SettlementStatus.CONFIRMED,
            SettlementStatus.READY_FOR_PAYOUT,
            SettlementStatus.PAID,
            SettlementStatus.PAYOUT_FAILED
    );

    private static final EnumSet<SettlementStatus> RECALCULABLE_STATUSES = EnumSet.of(
            SettlementStatus.CALCULATED,
            SettlementStatus.HELD
    );

    private final SettlementRepository settlementRepository;
    private final DeliveryOrderRepository deliveryOrderRepository;
    private final BatchJobLogRepository batchJobLogRepository;
    private final UserAccountRepository userAccountRepository;
    private final AgencyRepository agencyRepository;
    private final PolicyResolver policyResolver;

    @Transactional
    public SettlementBatchResponse runBatch(AuthUser actor, SettlementBatchRequest request) {
        if (actor.role() != UserRole.ADMIN) {
            throw BusinessException.forbidden("Only ADMIN can run settlement batch");
        }
        if (!request.periodStart().isBefore(request.periodEnd())) {
            throw BusinessException.badRequest("periodStart must be before periodEnd");
        }

        UserAccount triggeredBy = userAccountRepository.findById(actor.id())
                .orElseThrow(() -> BusinessException.notFound("User not found"));
        Agency agencyFilter = null;
        if (request.agencyId() != null) {
            agencyFilter = agencyRepository.findById(request.agencyId())
                    .orElseThrow(() -> BusinessException.notFound("Agency not found"));
        }

        BatchJobLog jobLog = batchJobLogRepository.save(BatchJobLog.start(
                request.periodStart(),
                request.periodEnd(),
                agencyFilter,
                triggeredBy,
                LocalDateTime.now()
        ));

        try {
            assertNoFinalizedSettlements(request);
            clearRecalculableSettlements(request);

            List<DeliveryOrder> orders = findTargetOrders(request);
            Map<String, List<DeliveryOrder>> grouped = orders.stream()
                    .collect(Collectors.groupingBy(
                            o -> o.getAgency().getId() + ":" + o.getMerchant().getId(),
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            int created = 0;
            for (List<DeliveryOrder> group : grouped.values()) {
                settlementRepository.save(buildSettlement(group, request.periodStart(), request.periodEnd()));
                created++;
            }

            for (DeliveryOrder order : orders) {
                order.lockForSettlement();
            }

            jobLog.succeed(orders.size(), created, LocalDateTime.now());
            return new SettlementBatchResponse(
                    jobLog.getId(),
                    BatchJobStatus.SUCCESS,
                    orders.size(),
                    created,
                    null
            );
        } catch (RuntimeException ex) {
            jobLog.fail(ex.getMessage(), LocalDateTime.now());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public SettlementBatchResponse getBatchJob(AuthUser actor, Long batchJobId) {
        if (actor.role() != UserRole.ADMIN) {
            throw BusinessException.forbidden("Only ADMIN can view batch jobs");
        }
        BatchJobLog job = batchJobLogRepository.findById(batchJobId)
                .orElseThrow(() -> BusinessException.notFound("Batch job not found"));
        return new SettlementBatchResponse(
                job.getId(),
                job.getStatus(),
                job.getProcessedOrderCount(),
                job.getCreatedSettlementCount(),
                job.getErrorMessage()
        );
    }

    private void assertNoFinalizedSettlements(SettlementBatchRequest request) {
        boolean exists = request.agencyId() == null
                ? settlementRepository.existsByPeriodStartAndPeriodEndAndStatusIn(
                request.periodStart(), request.periodEnd(), FINALIZED_STATUSES)
                : settlementRepository.existsByAgencyIdAndPeriodStartAndPeriodEndAndStatusIn(
                request.agencyId(), request.periodStart(), request.periodEnd(), FINALIZED_STATUSES);
        if (exists) {
            throw BusinessException.invalidState("Cannot recalculate period with confirmed/paid settlements");
        }
    }

    private void clearRecalculableSettlements(SettlementBatchRequest request) {
        List<Settlement> existing = request.agencyId() == null
                ? settlementRepository.findByPeriodStartAndPeriodEndAndStatusIn(
                request.periodStart(), request.periodEnd(), RECALCULABLE_STATUSES)
                : settlementRepository.findByAgencyIdAndPeriodStartAndPeriodEndAndStatusIn(
                request.agencyId(), request.periodStart(), request.periodEnd(), RECALCULABLE_STATUSES);

        for (Settlement settlement : existing) {
            for (SettlementLine line : settlement.getLines()) {
                line.getOrder().unlockFromSettlement();
            }
            settlementRepository.delete(settlement);
        }
        settlementRepository.flush();
    }

    private List<DeliveryOrder> findTargetOrders(SettlementBatchRequest request) {
        if (request.agencyId() == null) {
            return deliveryOrderRepository
                    .findByStatusAndSettlementLockedFalseAndOrderedAtGreaterThanEqualAndOrderedAtLessThan(
                            OrderStatus.CREATED, request.periodStart(), request.periodEnd());
        }
        return deliveryOrderRepository
                .findByAgencyIdAndStatusAndSettlementLockedFalseAndOrderedAtGreaterThanEqualAndOrderedAtLessThan(
                        request.agencyId(), OrderStatus.CREATED, request.periodStart(), request.periodEnd());
    }

    private Settlement buildSettlement(
            List<DeliveryOrder> orders,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    ) {
        Agency agency = orders.get(0).getAgency();
        Merchant merchant = orders.get(0).getMerchant();

        Set<AnomalyFlag> anomalyFlags = new LinkedHashSet<>();
        long totalOrderAmount = 0;
        long totalPlatform = 0;
        long totalAgency = 0;
        long totalRider = 0;
        long totalTip = 0;
        long totalMerchant = 0;

        Settlement settlement = Settlement.builder()
                .agency(agency)
                .merchant(merchant)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .status(SettlementStatus.CALCULATED)
                .orderCount(orders.size())
                .totalOrderAmount(0)
                .totalPlatformFeeAmount(0)
                .totalAgencySettlementAmount(0)
                .totalRiderFeeAmount(0)
                .totalTipAmount(0)
                .totalMerchantSettlementAmount(0)
                .build();

        for (DeliveryOrder order : orders) {
            SettlementBreakdown breakdown = calculate(order);
            breakdown.anomalyFlag().ifPresent(anomalyFlags::add);

            totalOrderAmount += breakdown.orderAmount();
            totalPlatform += breakdown.platformFeeAmount();
            totalAgency += breakdown.agencySettlementAmount();
            totalRider += breakdown.riderFeeAmount();
            totalTip += breakdown.tipToRiderAmount();
            totalMerchant += breakdown.merchantSettlementAmount();

            settlement.addLine(SettlementLine.from(order, breakdown));
        }

        // totals via new builder values — Settlement fields are final-ish via lombok getter only.
        // We need a way to set totals. Currently no setters. Use reflection? Better add package method applyTotals.
        settlement.applyTotals(
                orders.size(),
                totalOrderAmount,
                totalPlatform,
                totalAgency,
                totalRider,
                totalTip,
                totalMerchant,
                anomalyFlags.isEmpty()
                        ? null
                        : anomalyFlags.stream().map(Enum::name).collect(Collectors.joining(","))
        );

        boolean shouldHold = anomalyFlags.contains(AnomalyFlag.POLICY_MISSING)
                || anomalyFlags.contains(AnomalyFlag.NEGATIVE_MERCHANT_AMOUNT);
        if (shouldHold) {
            settlement.hold("Auto-held due to anomalies: "
                    + anomalyFlags.stream().map(Enum::name).collect(Collectors.joining(",")));
        }
        return settlement;
    }

    private SettlementBreakdown calculate(DeliveryOrder order) {
        Optional<FeePolicy> policy = policyResolver.resolve(order.getAgency().getId(), order.getOrderedAt());
        if (policy.isEmpty()) {
            return SettlementBreakdown.policyMissing(order.getOrderAmount(), order.getDeliveryTip());
        }
        FeePolicy feePolicy = policy.get();
        return SettlementCalculator.calculate(new SettlementCalculateCommand(
                order.getOrderAmount(),
                order.getDeliveryTip(),
                feePolicy.getPlatformFeeBps(),
                feePolicy.getAgencyFeeBps(),
                feePolicy.getRiderFee()
        ));
    }
}
