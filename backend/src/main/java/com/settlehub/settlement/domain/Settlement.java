package com.settlehub.settlement.domain;

import com.settlehub.common.persistence.BaseTimeEntity;
import com.settlehub.organization.domain.Agency;
import com.settlehub.organization.domain.Merchant;
import com.settlehub.organization.domain.UserAccount;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "settlements",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_settlement_period",
                columnNames = {"agency_id", "merchant_id", "period_start", "period_end"}
        ),
        indexes = {
                @Index(name = "idx_settlement_agency_period", columnList = "agency_id, period_start"),
                @Index(name = "idx_settlement_status", columnList = "status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Settlement extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "period_start", nullable = false, columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false, columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SettlementStatus status;

    @Column(name = "order_count", nullable = false)
    private int orderCount;

    @Column(name = "total_order_amount", nullable = false)
    private long totalOrderAmount;

    @Column(name = "total_platform_fee_amount", nullable = false)
    private long totalPlatformFeeAmount;

    @Column(name = "total_agency_settlement_amount", nullable = false)
    private long totalAgencySettlementAmount;

    @Column(name = "total_rider_fee_amount", nullable = false)
    private long totalRiderFeeAmount;

    @Column(name = "total_tip_amount", nullable = false)
    private long totalTipAmount;

    @Column(name = "total_merchant_settlement_amount", nullable = false)
    private long totalMerchantSettlementAmount;

    @Column(name = "anomaly_flags", length = 255)
    private String anomalyFlags;

    @Column(name = "hold_reason", length = 500)
    private String holdReason;

    @Column(name = "confirmed_at", columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime confirmedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private UserAccount confirmedBy;

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementLine> lines = new ArrayList<>();

    @Builder
    private Settlement(
            Agency agency,
            Merchant merchant,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            SettlementStatus status,
            int orderCount,
            long totalOrderAmount,
            long totalPlatformFeeAmount,
            long totalAgencySettlementAmount,
            long totalRiderFeeAmount,
            long totalTipAmount,
            long totalMerchantSettlementAmount,
            String anomalyFlags
    ) {
        this.agency = agency;
        this.merchant = merchant;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.status = status;
        this.orderCount = orderCount;
        this.totalOrderAmount = totalOrderAmount;
        this.totalPlatformFeeAmount = totalPlatformFeeAmount;
        this.totalAgencySettlementAmount = totalAgencySettlementAmount;
        this.totalRiderFeeAmount = totalRiderFeeAmount;
        this.totalTipAmount = totalTipAmount;
        this.totalMerchantSettlementAmount = totalMerchantSettlementAmount;
        this.anomalyFlags = anomalyFlags;
    }

    public void addLine(SettlementLine line) {
        lines.add(line);
        line.assignSettlement(this);
    }

    public void applyTotals(
            int orderCount,
            long totalOrderAmount,
            long totalPlatformFeeAmount,
            long totalAgencySettlementAmount,
            long totalRiderFeeAmount,
            long totalTipAmount,
            long totalMerchantSettlementAmount,
            String anomalyFlags
    ) {
        this.orderCount = orderCount;
        this.totalOrderAmount = totalOrderAmount;
        this.totalPlatformFeeAmount = totalPlatformFeeAmount;
        this.totalAgencySettlementAmount = totalAgencySettlementAmount;
        this.totalRiderFeeAmount = totalRiderFeeAmount;
        this.totalTipAmount = totalTipAmount;
        this.totalMerchantSettlementAmount = totalMerchantSettlementAmount;
        this.anomalyFlags = anomalyFlags;
    }

    public void hold(String reason) {
        if (status != SettlementStatus.CALCULATED) {
            throw new IllegalStateException("Only CALCULATED settlements can be held");
        }
        this.status = SettlementStatus.HELD;
        this.holdReason = reason;
    }

    public void confirm(UserAccount confirmer, LocalDateTime confirmedAt) {
        if (status != SettlementStatus.CALCULATED && status != SettlementStatus.HELD) {
            throw new IllegalStateException("Invalid status for confirm: " + status);
        }
        this.status = SettlementStatus.CONFIRMED;
        this.confirmedBy = confirmer;
        this.confirmedAt = confirmedAt;
        this.holdReason = null;
    }

    public void markReadyForPayout() {
        if (status != SettlementStatus.CONFIRMED) {
            throw new IllegalStateException("Only CONFIRMED settlements can be ready for payout");
        }
        this.status = SettlementStatus.READY_FOR_PAYOUT;
    }

    public void assertPayable() {
        if (status != SettlementStatus.READY_FOR_PAYOUT && status != SettlementStatus.PAYOUT_FAILED) {
            throw new IllegalStateException("Settlement is not ready for payout: " + status);
        }
    }

    public void markPaid() {
        assertPayable();
        this.status = SettlementStatus.PAID;
    }

    public void markPayoutFailed() {
        if (status != SettlementStatus.READY_FOR_PAYOUT && status != SettlementStatus.PAYOUT_FAILED) {
            throw new IllegalStateException("Settlement is not ready for payout: " + status);
        }
        this.status = SettlementStatus.PAYOUT_FAILED;
    }
}
