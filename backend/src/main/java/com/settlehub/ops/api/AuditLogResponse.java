package com.settlehub.ops.api;

import com.settlehub.ops.domain.AuditLog;

import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        Long actorUserId,
        String actorEmail,
        String action,
        String entityType,
        Long entityId,
        String beforeJson,
        String afterJson,
        String reason,
        LocalDateTime createdAt
) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActor() == null ? null : log.getActor().getId(),
                log.getActor() == null ? null : log.getActor().getEmail(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getBeforeJson(),
                log.getAfterJson(),
                log.getReason(),
                log.getCreatedAt()
        );
    }
}
