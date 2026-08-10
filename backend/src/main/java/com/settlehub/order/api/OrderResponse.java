package com.settlehub.order.api;

import com.settlehub.order.domain.DeliveryOrder;
import com.settlehub.order.domain.OrderStatus;

import java.time.LocalDateTime;

public record OrderResponse(
        Long id,
        Long agencyId,
        String agencyCode,
        Long merchantId,
        String merchantCode,
        String merchantName,
        String externalOrderId,
        long orderAmount,
        long deliveryTip,
        LocalDateTime orderedAt,
        OrderStatus status,
        boolean settlementLocked,
        LocalDateTime createdAt
) {

    public static OrderResponse from(DeliveryOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getAgency().getId(),
                order.getAgency().getCode(),
                order.getMerchant().getId(),
                order.getMerchant().getCode(),
                order.getMerchant().getName(),
                order.getExternalOrderId(),
                order.getOrderAmount(),
                order.getDeliveryTip(),
                order.getOrderedAt(),
                order.getStatus(),
                order.isSettlementLocked(),
                order.getCreatedAt()
        );
    }
}
