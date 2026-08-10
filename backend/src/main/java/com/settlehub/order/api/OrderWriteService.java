package com.settlehub.order.api;

import com.settlehub.common.exception.BusinessException;
import com.settlehub.organization.domain.Agency;
import com.settlehub.organization.domain.AgencyRepository;
import com.settlehub.organization.domain.Merchant;
import com.settlehub.organization.domain.MerchantRepository;
import com.settlehub.order.domain.DeliveryOrder;
import com.settlehub.order.domain.DeliveryOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderWriteService {

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final AgencyRepository agencyRepository;
    private final MerchantRepository merchantRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistCsvRow(Long agencyId, OrderCsvParser.ParsedCsvRow row) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> BusinessException.notFound("Agency not found"));

        Merchant merchant = merchantRepository.findByAgencyIdAndCode(agency.getId(), row.merchantCode())
                .orElseThrow(() -> BusinessException.notFound("MERCHANT_NOT_FOUND"));
        if (!merchant.isActive()) {
            throw BusinessException.invalidState("MERCHANT_INACTIVE");
        }
        if (deliveryOrderRepository.existsByAgencyIdAndExternalOrderId(agency.getId(), row.externalOrderId())) {
            throw BusinessException.conflict("DUPLICATE_EXTERNAL_ORDER_ID", "DUPLICATE_EXTERNAL_ORDER_ID");
        }

        DeliveryOrder order = DeliveryOrder.create(
                agency,
                merchant,
                row.externalOrderId(),
                row.orderAmount(),
                row.deliveryTip(),
                row.orderedAt(),
                row.status()
        );
        deliveryOrderRepository.saveAndFlush(order);
    }
}
