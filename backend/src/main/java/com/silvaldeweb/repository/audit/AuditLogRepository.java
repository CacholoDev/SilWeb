package com.silvaldeweb.repository.audit;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.silvaldeweb.model.audit.Action;
import com.silvaldeweb.model.audit.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByActorId(Long actorId, Pageable pageable);

    Page<AuditLog> findByEntityNameAndEntityId(String entityName, Long entityId, Pageable pageable);

    Page<AuditLog> findByEntityName(String entityName, Pageable pageable);

    Page<AuditLog> findByAction(Action action, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetween(Instant from, Instant to, Pageable pageable);

    List<AuditLog> findByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, Long entityId);
}
