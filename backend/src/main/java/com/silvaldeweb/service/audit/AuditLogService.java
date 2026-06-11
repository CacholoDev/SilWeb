package com.silvaldeweb.service.audit;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.silvaldeweb.dto.audit.AuditLogResponse;
import com.silvaldeweb.model.audit.Action;
import com.silvaldeweb.model.audit.AuditLog;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.audit.AuditLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(User actor, Action action, String entityName, Long entityId, String metadata, String ipAddress) {
        if (actor == null) {
            log.debug("Skipping audit log: no actor (entity={} id={} action={})", entityName, entityId, action);
            return;
        }
        if (actor.getRole() != Role.ADMIN) {
            log.debug("Skipping audit log: actor is not ADMIN (email={} role={})", actor.getEmail(), actor.getRole());
            return;
        }

        try {
            AuditLog auditLog = AuditLog.builder()
                    .actor(actor)
                    .action(action)
                    .entityName(entityName)
                    .entityId(entityId)
                    .metadata(metadata)
                    .ipAddress(ipAddress)
                    .build();
            auditLogRepository.save(auditLog);
            log.debug("Audit log recorded actor={} action={} entity={}#{}", actor.getEmail(), action, entityName, entityId);
        } catch (RuntimeException exception) {
            log.error("Failed to record audit log actor={} action={} entity={}#{}",
                    actor.getEmail(), action, entityName, entityId, exception);
        }
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(Long actorId, String entityName, Long entityId,
                                        Action action, Instant from, Instant to, Pageable pageable) {
        log.info("Listing audit logs actorId={} entity={}#{} action={} from={} to={} page={}",
                actorId, entityName, entityId, action, from, to, pageable.getPageNumber());

        Page<AuditLog> page;
        if (actorId != null) {
            page = auditLogRepository.findByActorId(actorId, pageable);
        } else if (entityName != null && entityId != null) {
            page = auditLogRepository.findByEntityNameAndEntityId(entityName, entityId, pageable);
        } else if (entityName != null) {
            page = auditLogRepository.findByEntityName(entityName, pageable);
        } else if (action != null) {
            page = auditLogRepository.findByAction(action, pageable);
        } else if (from != null && to != null) {
            page = auditLogRepository.findByCreatedAtBetween(from, to, pageable);
        } else {
            page = auditLogRepository.findAll(pageable);
        }
        return page.map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getActor() != null ? auditLog.getActor().getId() : null,
                auditLog.getActor() != null ? auditLog.getActor().getEmail() : null,
                auditLog.getAction(),
                auditLog.getEntityName(),
                auditLog.getEntityId(),
                auditLog.getMetadata(),
                auditLog.getIpAddress(),
                auditLog.getCreatedAt()
        );
    }
}
