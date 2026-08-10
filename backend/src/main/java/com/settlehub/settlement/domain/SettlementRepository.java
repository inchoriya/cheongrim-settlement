package com.settlehub.settlement.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long>,
        JpaSpecificationExecutor<Settlement> {

    Optional<Settlement> findByAgencyIdAndMerchantIdAndPeriodStartAndPeriodEnd(
            Long agencyId,
            Long merchantId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    );

    List<Settlement> findByPeriodStartAndPeriodEndAndStatusIn(
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            Collection<SettlementStatus> statuses
    );

    List<Settlement> findByAgencyIdAndPeriodStartAndPeriodEndAndStatusIn(
            Long agencyId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            Collection<SettlementStatus> statuses
    );

    boolean existsByPeriodStartAndPeriodEndAndStatusIn(
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            Collection<SettlementStatus> statuses
    );

    boolean existsByAgencyIdAndPeriodStartAndPeriodEndAndStatusIn(
            Long agencyId,
            LocalDateTime periodStart,
            LocalDateTime periodEnd,
            Collection<SettlementStatus> statuses
    );

    @Query("""
            select distinct s from Settlement s
            left join fetch s.lines l
            left join fetch l.order
            where s.id = :id
            """)
    Optional<Settlement> findDetailById(@Param("id") Long id);
}
