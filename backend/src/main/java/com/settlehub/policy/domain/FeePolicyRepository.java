package com.settlehub.policy.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeePolicyRepository extends JpaRepository<FeePolicy, Long> {

    List<FeePolicy> findByAgencyIsNullAndActiveTrue();

    List<FeePolicy> findByAgencyIdAndActiveTrue(Long agencyId);

    @Query("""
            select p from FeePolicy p
            where p.active = true
              and p.agency.id = :agencyId
              and p.effectiveFrom <= :at
              and (p.effectiveTo is null or p.effectiveTo > :at)
            order by p.effectiveFrom desc
            """)
    List<FeePolicy> findEffectiveAgencyPolicies(@Param("agencyId") Long agencyId, @Param("at") LocalDateTime at);

    @Query("""
            select p from FeePolicy p
            where p.active = true
              and p.agency is null
              and p.effectiveFrom <= :at
              and (p.effectiveTo is null or p.effectiveTo > :at)
            order by p.effectiveFrom desc
            """)
    List<FeePolicy> findEffectiveGlobalPolicies(@Param("at") LocalDateTime at);

    default Optional<FeePolicy> findEffectiveAgencyPolicy(Long agencyId, LocalDateTime at) {
        return findEffectiveAgencyPolicies(agencyId, at).stream().findFirst();
    }

    default Optional<FeePolicy> findEffectiveGlobalPolicy(LocalDateTime at) {
        return findEffectiveGlobalPolicies(at).stream().findFirst();
    }
}
