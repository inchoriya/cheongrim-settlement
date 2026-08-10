package com.settlehub.payout.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PayoutTransactionRepository extends JpaRepository<PayoutTransaction, Long>,
        JpaSpecificationExecutor<PayoutTransaction> {

    List<PayoutTransaction> findBySettlementId(Long settlementId);

    boolean existsBySettlementIdAndStatus(Long settlementId, PayoutStatus status);
}
