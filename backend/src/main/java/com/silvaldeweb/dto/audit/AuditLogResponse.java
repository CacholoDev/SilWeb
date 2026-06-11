package com.silvaldeweb.dto.audit;

import java.time.Instant;

import com.silvaldeweb.model.audit.Action;

public record AuditLogResponse(
        Long id,
        Long actorId,
        String actorEmail,
        Action action,
        String entityName,
        Long entityId,
        String metadata,
        String ipAddress,
        Instant createdAt
) {
}
