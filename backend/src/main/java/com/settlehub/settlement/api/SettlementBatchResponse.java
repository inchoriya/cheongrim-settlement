package com.settlehub.settlement.api;

import com.settlehub.ops.domain.BatchJobStatus;

public record SettlementBatchResponse(
        Long batchJobId,
        BatchJobStatus status,
        int processedOrderCount,
        int createdSettlementCount,
        String errorMessage
) {
}
