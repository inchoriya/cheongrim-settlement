package com.settlehub.order.api;

import java.util.List;

public record OrderUploadResponse(
        int totalRows,
        int successCount,
        int failureCount,
        List<OrderUploadFailure> failures
) {
}
