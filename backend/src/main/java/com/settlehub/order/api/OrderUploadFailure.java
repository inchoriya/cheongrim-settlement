package com.settlehub.order.api;

public record OrderUploadFailure(
        int row,
        String externalOrderId,
        String reason
) {
}
