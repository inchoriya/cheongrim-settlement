package com.settlehub.order.domain;

import com.settlehub.common.persistence.BaseTimeEntity;
import com.settlehub.organization.domain.Agency;
import com.settlehub.organization.domain.Merchant;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "delivery_orders",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_agency_external_id",
                columnNames = {"agency_id", "external_order_id"}
        ),
        indexes = {
                @Index(name = "idx_order_agency_ordered_at", columnList = "agency_id, ordered_at"),
                @Index(name = "idx_order_merchant_ordered_at", columnList = "merchant_id, ordered_at"),
                @Index(name = "idx_order_status_ordered_at", columnList = "status, ordered_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "external_order_id", nullable = false, length = 100)
    private String externalOrderId;

    @Column(name = "order_amount", nullable = false)
    private long orderAmount;

    @Column(name = "delivery_tip", nullable = false)
    private long deliveryTip;

    @Column(name = "ordered_at", nullable = false, columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime orderedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "settlement_locked", nullable = false)
    private boolean settlementLocked;

    @Builder
    private DeliveryOrder(
            Agency agency,
            Merchant merchant,
            String externalOrderId,
            long orderAmount,
            long deliveryTip,
            LocalDateTime orderedAt,
            OrderStatus status,
            boolean settlementLocked
    ) {
        this.agency = agency;
        this.merchant = merchant;
        this.externalOrderId = externalOrderId;
        this.orderAmount = orderAmount;
        this.deliveryTip = deliveryTip;
        this.orderedAt = orderedAt;
        this.status = status;
        this.settlementLocked = settlementLocked;
    }

    public static DeliveryOrder create(
            Agency agency,
            Merchant merchant,
            String externalOrderId,
            long orderAmount,
            long deliveryTip,
            LocalDateTime orderedAt
    ) {
        return create(agency, merchant, externalOrderId, orderAmount, deliveryTip, orderedAt, OrderStatus.CREATED);
    }

    public static DeliveryOrder create(
            Agency agency,
            Merchant merchant,
            String externalOrderId,
            long orderAmount,
            long deliveryTip,
            LocalDateTime orderedAt,
            OrderStatus status
    ) {
        return DeliveryOrder.builder()
                .agency(agency)
                .merchant(merchant)
                .externalOrderId(externalOrderId)
                .orderAmount(orderAmount)
                .deliveryTip(deliveryTip)
                .orderedAt(orderedAt)
                .status(status == null ? OrderStatus.CREATED : status)
                .settlementLocked(false)
                .build();
    }

    public void cancel() {
        if (settlementLocked) {
            throw new IllegalStateException("Cannot cancel a settlement-locked order");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order is already cancelled");
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void lockForSettlement() {
        this.settlementLocked = true;
    }

    public void unlockFromSettlement() {
        this.settlementLocked = false;
    }
}
