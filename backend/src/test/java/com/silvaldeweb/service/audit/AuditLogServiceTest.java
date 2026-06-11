package com.silvaldeweb.service.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.silvaldeweb.model.audit.Action;
import com.silvaldeweb.model.audit.AuditLog;
import com.silvaldeweb.model.user.Role;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.audit.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private User adminUser() {
        return User.builder().id(1L).email("admin@example.com").role(Role.ADMIN).active(true).build();
    }

    private User regularUser() {
        return User.builder().id(2L).email("customer@example.com").role(Role.USER).active(true).build();
    }

    @Test
    void recordPersistsLogForAdmin() {
        User admin = adminUser();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(i -> {
            AuditLog log = i.getArgument(0);
            log.setId(99L);
            return log;
        });

        auditLogService.record(admin, Action.DELETE, "Order", 10L, "metadata", "127.0.0.1");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals(admin, saved.getActor());
        assertEquals(Action.DELETE, saved.getAction());
        assertEquals("Order", saved.getEntityName());
        assertEquals(10L, saved.getEntityId());
        assertEquals("metadata", saved.getMetadata());
        assertEquals("127.0.0.1", saved.getIpAddress());
    }

    @Test
    void recordSkipsForRegularUser() {
        auditLogService.record(regularUser(), Action.DELETE, "Order", 10L, null, null);
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void recordSkipsForNullActor() {
        auditLogService.record(null, Action.LOGIN, "User", null, null, null);
        verify(auditLogRepository, never()).save(any(AuditLog.class));
    }

    @Test
    void recordSwallowsRepositoryFailure() {
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("DB down"));
        auditLogService.record(adminUser(), Action.DELETE, "Order", 10L, null, null);
    }

    @Test
    void listWithNoFiltersReturnsAll() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> page = new PageImpl<>(List.of(), pageable, 0);
        when(auditLogRepository.findAll(pageable)).thenReturn(page);

        var response = auditLogService.list(null, null, null, null, null, null, pageable);

        assertEquals(0, response.getTotalElements());
    }

    @Test
    void listByActorUsesActorQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> page = new PageImpl<>(List.of(), pageable, 0);
        when(auditLogRepository.findByActorId(1L, pageable)).thenReturn(page);

        var response = auditLogService.list(1L, null, null, null, null, null, pageable);

        assertEquals(0, response.getTotalElements());
        verify(auditLogRepository).findByActorId(1L, pageable);
    }

    @Test
    void listByEntityUsesEntityQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> page = new PageImpl<>(List.of(), pageable, 0);
        when(auditLogRepository.findByEntityNameAndEntityId("Order", 10L, pageable)).thenReturn(page);

        auditLogService.list(null, "Order", 10L, null, null, null, pageable);

        verify(auditLogRepository).findByEntityNameAndEntityId("Order", 10L, pageable);
    }

    @Test
    void listByEntityNameOnlyUsesNameQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> page = new PageImpl<>(List.of(), pageable, 0);
        when(auditLogRepository.findByEntityName("Product", pageable)).thenReturn(page);

        auditLogService.list(null, "Product", null, null, null, null, pageable);

        verify(auditLogRepository).findByEntityName("Product", pageable);
    }

    @Test
    void listByActionUsesActionQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> page = new PageImpl<>(List.of(), pageable, 0);
        when(auditLogRepository.findByAction(Action.DELETE, pageable)).thenReturn(page);

        auditLogService.list(null, null, null, Action.DELETE, null, null, pageable);

        verify(auditLogRepository).findByAction(Action.DELETE, pageable);
    }

    @Test
    void listByDateRangeUsesBetweenQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> page = new PageImpl<>(List.of(), pageable, 0);
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T23:59:59Z");
        when(auditLogRepository.findByCreatedAtBetween(from, to, pageable)).thenReturn(page);

        auditLogService.list(null, null, null, null, from, to, pageable);

        verify(auditLogRepository).findByCreatedAtBetween(from, to, pageable);
    }
}
