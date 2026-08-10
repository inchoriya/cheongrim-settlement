package com.settlehub.organization.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByAgencyIdAndCode(Long agencyId, String code);

    List<Merchant> findByAgencyId(Long agencyId);

    boolean existsByAgencyIdAndCode(Long agencyId, String code);
}
