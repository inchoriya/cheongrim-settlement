package com.settlehub.order.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeliveryOrderRepository extends JpaRepository<DeliveryOrder, Long>,
        JpaSpecificationExecutor<DeliveryOrder> {

    Optional<DeliveryOrder> findByAgencyIdAndExternalOrderId(Long agencyId, String externalOrderId);

    boolean existsByAgencyIdAndExternalOrderId(Long agencyId, String externalOrderId);

    List<DeliveryOrder> findByStatusAndSettlementLockedFalseAndOrderedAtGreaterThanEqualAndOrderedAtLessThan(
            OrderStatus status,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    );

    List<DeliveryOrder> findByAgencyIdAndStatusAndSettlementLockedFalseAndOrderedAtGreaterThanEqualAndOrderedAtLessThan(
            Long agencyId,
            OrderStatus status,
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    );
}
