package com.settlehub.order.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.api.PageResponse;
import com.settlehub.common.exception.BusinessException;
import com.settlehub.organization.domain.Agency;
import com.settlehub.organization.domain.AgencyRepository;
import com.settlehub.organization.domain.Merchant;
import com.settlehub.organization.domain.MerchantRepository;
import com.settlehub.organization.domain.UserRole;
import com.settlehub.order.domain.DeliveryOrder;
import com.settlehub.order.domain.DeliveryOrderRepository;
import com.settlehub.order.domain.OrderStatus;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final DeliveryOrderRepository deliveryOrderRepository;
    private final AgencyRepository agencyRepository;
    private final MerchantRepository merchantRepository;
    private final OrderCsvParser orderCsvParser;
    private final OrderWriteService orderWriteService;

    @Transactional
    public OrderResponse create(AuthUser actor, OrderCreateRequest request) {
        assertCanWrite(actor);

        Long agencyId = resolveAgencyIdForWrite(actor, request.agencyId());
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> BusinessException.notFound("Agency not found"));
        if (!agency.isActive()) {
            throw BusinessException.invalidState("Agency is inactive");
        }

        Merchant merchant = merchantRepository.findById(request.merchantId())
                .orElseThrow(() -> BusinessException.notFound("Merchant not found"));
        if (!merchant.getAgency().getId().equals(agencyId)) {
            throw BusinessException.badRequest("Merchant does not belong to agency");
        }
        if (!merchant.isActive()) {
            throw BusinessException.invalidState("Merchant is inactive");
        }

        if (deliveryOrderRepository.existsByAgencyIdAndExternalOrderId(agencyId, request.externalOrderId())) {
            throw BusinessException.conflict("DUPLICATE_RESOURCE", "DUPLICATE_EXTERNAL_ORDER_ID");
        }

        long deliveryTip = request.deliveryTip() == null ? 0L : request.deliveryTip();
        OrderStatus status = request.status() == null ? OrderStatus.CREATED : request.status();

        DeliveryOrder order = DeliveryOrder.create(
                agency,
                merchant,
                request.externalOrderId(),
                request.orderAmount(),
                deliveryTip,
                request.orderedAt(),
                status
        );
        return OrderResponse.from(deliveryOrderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderUploadResponse upload(AuthUser actor, Long agencyIdParam, MultipartFile file) {
        assertCanWrite(actor);
        Long agencyId = resolveAgencyIdForWrite(actor, agencyIdParam);

        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> BusinessException.notFound("Agency not found"));
        if (!agency.isActive()) {
            throw BusinessException.invalidState("Agency is inactive");
        }

        List<OrderCsvParser.ParsedCsvRow> rows = orderCsvParser.parse(file);
        List<OrderUploadFailure> failures = new ArrayList<>();
        int successCount = 0;

        for (OrderCsvParser.ParsedCsvRow row : rows) {
            if (!row.valid()) {
                failures.add(new OrderUploadFailure(row.rowNumber(), row.externalOrderId(), row.errorReason()));
                continue;
            }
            try {
                orderWriteService.persistCsvRow(agency.getId(), row);
                successCount++;
            } catch (BusinessException ex) {
                failures.add(new OrderUploadFailure(row.rowNumber(), row.externalOrderId(), ex.getCode()));
            } catch (RuntimeException ex) {
                failures.add(new OrderUploadFailure(row.rowNumber(), row.externalOrderId(), "SAVE_FAILED"));
            }
        }

        return new OrderUploadResponse(rows.size(), successCount, failures.size(), failures);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> list(
            AuthUser actor,
            LocalDateTime from,
            LocalDateTime to,
            Long merchantId,
            OrderStatus status,
            Pageable pageable
    ) {
        Specification<DeliveryOrder> spec = buildListSpec(actor, from, to, merchantId, status);
        Page<OrderResponse> page = deliveryOrderRepository.findAll(spec, pageable).map(OrderResponse::from);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public OrderResponse get(AuthUser actor, Long orderId) {
        DeliveryOrder order = deliveryOrderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("Order not found"));
        assertCanRead(actor, order);
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancel(AuthUser actor, Long orderId) {
        assertCanWrite(actor);
        DeliveryOrder order = deliveryOrderRepository.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("Order not found"));
        assertCanRead(actor, order);
        if (actor.role() == UserRole.AGENCY
                && (actor.agencyId() == null || !actor.agencyId().equals(order.getAgency().getId()))) {
            throw BusinessException.forbidden("Cannot cancel orders outside your agency");
        }
        order.cancel();
        return OrderResponse.from(order);
    }

    private Specification<DeliveryOrder> buildListSpec(
            AuthUser actor,
            LocalDateTime from,
            LocalDateTime to,
            Long merchantId,
            OrderStatus status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            switch (actor.role()) {
                case ADMIN -> {
                    // no org restriction
                }
                case AGENCY -> {
                    if (actor.agencyId() == null) {
                        throw BusinessException.forbidden("Agency scope missing");
                    }
                    predicates.add(cb.equal(root.get("agency").get("id"), actor.agencyId()));
                }
                case MERCHANT -> {
                    if (actor.merchantId() == null) {
                        throw BusinessException.forbidden("Merchant scope missing");
                    }
                    predicates.add(cb.equal(root.get("merchant").get("id"), actor.merchantId()));
                }
            }

            if (merchantId != null) {
                if (actor.role() == UserRole.MERCHANT && !merchantId.equals(actor.merchantId())) {
                    throw BusinessException.forbidden("Cannot query other merchant orders");
                }
                predicates.add(cb.equal(root.get("merchant").get("id"), merchantId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("orderedAt"), to));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void assertCanWrite(AuthUser actor) {
        if (actor.role() == UserRole.MERCHANT) {
            throw BusinessException.forbidden("Merchants cannot create or modify orders");
        }
    }

    private void assertCanRead(AuthUser actor, DeliveryOrder order) {
        switch (actor.role()) {
            case ADMIN -> {
            }
            case AGENCY -> {
                if (actor.agencyId() == null || !actor.agencyId().equals(order.getAgency().getId())) {
                    throw BusinessException.forbidden("Cannot access orders outside your agency");
                }
            }
            case MERCHANT -> {
                if (actor.merchantId() == null || !actor.merchantId().equals(order.getMerchant().getId())) {
                    throw BusinessException.forbidden("Cannot access orders outside your merchant");
                }
            }
        }
    }

    private Long resolveAgencyIdForWrite(AuthUser actor, Long requestedAgencyId) {
        if (actor.role() == UserRole.AGENCY) {
            if (actor.agencyId() == null) {
                throw BusinessException.forbidden("Agency scope missing");
            }
            if (requestedAgencyId != null && !requestedAgencyId.equals(actor.agencyId())) {
                throw BusinessException.forbidden("Cannot write orders for another agency");
            }
            return actor.agencyId();
        }
        if (requestedAgencyId == null) {
            throw BusinessException.badRequest("agencyId is required");
        }
        return requestedAgencyId;
    }
}
