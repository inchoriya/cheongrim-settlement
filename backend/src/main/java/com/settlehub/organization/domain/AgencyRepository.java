package com.settlehub.organization.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgencyRepository extends JpaRepository<Agency, Long> {

    Optional<Agency> findByCode(String code);

    boolean existsByCode(String code);
}
