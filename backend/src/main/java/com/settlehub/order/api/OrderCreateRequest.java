package com.settlehub.order.api;

import com.settlehub.order.domain.OrderStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record OrderCreateRequest(
        @NotBlank String externalOrderId,
        Long agencyId,
        @NotNull Long merchantId,
        @NotNull @Min(0) Long orderAmount,
        @Min(0) Long deliveryTip,
        @NotNull LocalDateTime orderedAt,
        OrderStatus status
) {
}
