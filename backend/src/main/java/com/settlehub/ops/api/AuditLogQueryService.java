package com.settlehub.ops.api;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.api.PageResponse;
import com.settlehub.common.exception.BusinessException;
import com.settlehub.ops.domain.AuditLog;
import com.settlehub.ops.domain.AuditLogRepository;
import com.settlehub.organization.domain.UserRole;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(
            AuthUser actor,
            String entityType,
            Long entityId,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        if (actor.role() != UserRole.ADMIN) {
            throw BusinessException.forbidden("Only ADMIN can view audit logs");
        }

        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<AuditLogResponse> page = auditLogRepository.findAll(spec, pageable).map(AuditLogResponse::from);
        return PageResponse.from(page);
    }
}
