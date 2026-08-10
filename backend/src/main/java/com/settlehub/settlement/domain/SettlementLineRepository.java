package com.settlehub.settlement.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementLineRepository extends JpaRepository<SettlementLine, Long> {

    boolean existsByOrderId(Long orderId);

    List<SettlementLine> findBySettlementId(Long settlementId);
}
