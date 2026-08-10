package com.settlehub.payout.domain;

import com.settlehub.settlement.domain.Settlement;
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
@Table(name = "payout_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PayoutTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "beneficiary_type", nullable = false, length = 20)
    private BeneficiaryType beneficiaryType;

    @Column(name = "beneficiary_id", nullable = false)
    private Long beneficiaryId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "pg_provider", nullable = false, length = 30)
    private PgProvider pgProvider;

    @Column(name = "pg_transaction_id", length = 100)
    private String pgTransactionId;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "requested_at", nullable = false, columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime requestedAt;

    @Column(name = "completed_at", columnDefinition = "TIMESTAMP(3)")
    private LocalDateTime completedAt;

    @Lob
    @Column(name = "raw_response")
    private String rawResponse;

    @Builder
    private PayoutTransaction(
            Settlement settlement,
            long amount,
            BeneficiaryType beneficiaryType,
            Long beneficiaryId,
            PayoutStatus status,
            PgProvider pgProvider,
            LocalDateTime requestedAt
    ) {
        this.settlement = settlement;
        this.amount = amount;
        this.beneficiaryType = beneficiaryType;
        this.beneficiaryId = beneficiaryId;
        this.status = status;
        this.pgProvider = pgProvider;
        this.requestedAt = requestedAt;
    }

    public static PayoutTransaction request(
            Settlement settlement,
            long amount,
            Long merchantId,
            PgProvider provider,
            LocalDateTime requestedAt
    ) {
        return PayoutTransaction.builder()
                .settlement(settlement)
                .amount(amount)
                .beneficiaryType(BeneficiaryType.MERCHANT)
                .beneficiaryId(merchantId)
                .status(PayoutStatus.REQUESTED)
                .pgProvider(provider)
                .requestedAt(requestedAt)
                .build();
    }

    public void succeed(String pgTransactionId, String rawResponse, LocalDateTime completedAt) {
        this.status = PayoutStatus.SUCCEEDED;
        this.pgTransactionId = pgTransactionId;
        this.rawResponse = rawResponse;
        this.completedAt = completedAt;
        this.failureReason = null;
    }

    public void fail(String reason, String rawResponse, LocalDateTime completedAt) {
        this.status = PayoutStatus.FAILED;
        this.failureReason = reason;
        this.rawResponse = rawResponse;
        this.completedAt = completedAt;
    }
}
