package com.settlehub.ops.domain;

import com.settlehub.organization.domain.Agency;
import com.settlehub.organization.domain.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "batch_job_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BatchJobLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private BatchJobType jobType;

    @Column(name = "period_start", nullable = false, columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false, columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime periodEnd;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchJobStatus status;

    @Column(name = "processed_order_count", nullable = false)
    private int processedOrderCount;

    @Column(name = "created_settlement_count", nullable = false)
    private int createdSettlementCount;

    @Lob
    @Column(name = "error_message")
    private String errorMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triggered_by")
    private UserAccount triggeredBy;

    @Column(name = "started_at", nullable = false, columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime startedAt;

    @Column(name = "finished_at", columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime finishedAt;

    @Builder
    private BatchJobLog(
            BatchJobType jobType,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            Agency agency,
            BatchJobStatus status,
            UserAccount triggeredBy,
            LocalDateTime startedAt
    ) {
        this.jobType = jobType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.agency = agency;
        this.status = status;
        this.triggeredBy = triggeredBy;
        this.startedAt = startedAt;
        this.processedOrderCount = 0;
        this.createdSettlementCount = 0;
    }

    public static BatchJobLog start(
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            Agency agency,
            UserAccount triggeredBy,
            LocalDateTime startedAt
    ) {
        return BatchJobLog.builder()
                .jobType(BatchJobType.WEEKLY_SETTLEMENT)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .agency(agency)
                .status(BatchJobStatus.RUNNING)
                .triggeredBy(triggeredBy)
                .startedAt(startedAt)
                .build();
    }

    public void succeed(int processedOrderCount, int createdSettlementCount, LocalDateTime finishedAt) {
        this.status = BatchJobStatus.SUCCESS;
        this.processedOrderCount = processedOrderCount;
        this.createdSettlementCount = createdSettlementCount;
        this.finishedAt = finishedAt;
    }

    public void fail(String errorMessage, LocalDateTime finishedAt) {
        this.status = BatchJobStatus.FAILED;
        this.errorMessage = errorMessage;
        this.finishedAt = finishedAt;
    }
}
