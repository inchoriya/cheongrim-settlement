package com.settlehub.ops;

import com.settlehub.ops.domain.AuditLog;
import com.settlehub.ops.domain.AuditLogRepository;
import com.settlehub.organization.domain.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void record(
            UserAccount actor,
            String action,
            String entityType,
            Long entityId,
            String beforeJson,
            String afterJson,
            String reason
    ) {
        auditLogRepository.save(AuditLog.builder()
                .actor(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .beforeJson(beforeJson)
                .afterJson(afterJson)
                .reason(reason)
                .build());
    }
}
