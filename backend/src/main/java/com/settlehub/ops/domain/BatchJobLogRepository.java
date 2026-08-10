package com.settlehub.ops.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchJobLogRepository extends JpaRepository<BatchJobLog, Long> {
}
