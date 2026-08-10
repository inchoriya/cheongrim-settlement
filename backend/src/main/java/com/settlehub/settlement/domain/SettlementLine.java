package com.settlehub.settlement.domain;

import com.settlehub.order.domain.DeliveryOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "settlement_lines",
        uniqueConstraints = @UniqueConstraint(name = "uk_settlement_line_order", columnNames = "order_id")
)
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private DeliveryOrder order;

    @Column(name = "order_amount", nullable = false)
    private long orderAmount;

    @Column(name = "delivery_tip", nullable = false)
    private long deliveryTip;

    @Column(name = "platform_fee_bps", nullable = false)
    private int platformFeeBps;

    @Column(name = "agency_fee_bps", nullable = false)
    private int agencyFeeBps;

    @Column(name = "rider_fee", nullable = false)
    private long riderFee;

    @Column(name = "platform_fee_amount", nullable = false)
    private long platformFeeAmount;

    @Column(name = "agency_fee_amount", nullable = false)
    private long agencyFeeAmount;

    @Column(name = "rider_fee_amount", nullable = false)
    private long riderFeeAmount;

    @Column(name = "tip_to_rider_amount", nullable = false)
    private long tipToRiderAmount;

    @Column(name = "merchant_settlement_amount", nullable = false)
    private long merchantSettlementAmount;

    @Column(name = "agency_settlement_amount", nullable = false)
    private long agencySettlementAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "anomaly_flag", length = 50)
    private AnomalyFlag anomalyFlag;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime createdAt;

    @Builder
    private SettlementLine(
            DeliveryOrder order,
            long orderAmount,
            long deliveryTip,
            int platformFeeBps,
            int agencyFeeBps,
            long riderFee,
            long platformFeeAmount,
            long agencyFeeAmount,
            long riderFeeAmount,
            long tipToRiderAmount,
            long merchantSettlementAmount,
            long agencySettlementAmount,
            AnomalyFlag anomalyFlag
    ) {
        this.order = order;
        this.orderAmount = orderAmount;
        this.deliveryTip = deliveryTip;
        this.platformFeeBps = platformFeeBps;
        this.agencyFeeBps = agencyFeeBps;
        this.riderFee = riderFee;
        this.platformFeeAmount = platformFeeAmount;
        this.agencyFeeAmount = agencyFeeAmount;
        this.riderFeeAmount = riderFeeAmount;
        this.tipToRiderAmount = tipToRiderAmount;
        this.merchantSettlementAmount = merchantSettlementAmount;
        this.agencySettlementAmount = agencySettlementAmount;
        this.anomalyFlag = anomalyFlag;
    }

    public static SettlementLine from(DeliveryOrder order, SettlementBreakdown breakdown) {
        return SettlementLine.builder()
                .order(order)
                .orderAmount(breakdown.orderAmount())
                .deliveryTip(breakdown.deliveryTip())
                .platformFeeBps(breakdown.platformFeeBps())
                .agencyFeeBps(breakdown.agencyFeeBps())
                .riderFee(breakdown.riderFee())
                .platformFeeAmount(breakdown.platformFeeAmount())
                .agencyFeeAmount(breakdown.agencyFeeAmount())
                .riderFeeAmount(breakdown.riderFeeAmount())
                .tipToRiderAmount(breakdown.tipToRiderAmount())
                .merchantSettlementAmount(breakdown.merchantSettlementAmount())
                .agencySettlementAmount(breakdown.agencySettlementAmount())
                .anomalyFlag(breakdown.anomalyFlag().orElse(null))
                .build();
    }

    void assignSettlement(Settlement settlement) {
        this.settlement = settlement;
    }
}
