package com.silvaldeweb.controller.audit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.silvaldeweb.dto.audit.AuditLogResponse;
import com.silvaldeweb.exception.GlobalExceptionHandler;
import com.silvaldeweb.model.audit.Action;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.audit.AuditLogService;

@ExtendWith(MockitoExtension.class)
class AuditLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuditLogController auditLogController;

    private Authentication adminAuth;
    private User adminUser;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(auditLogController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        adminUser = User.builder().id(1L).email("admin@example.com").active(true).build();
        adminAuth = new UsernamePasswordAuthenticationToken(
                "admin@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    private AuditLogResponse sampleLog(Long id) {
        return new AuditLogResponse(id, 1L, "admin@example.com", Action.DELETE,
                "Order", 50L, "metadata", "127.0.0.1",
                Instant.parse("2026-06-09T10:00:00Z"));
    }

    @Test
    void listReturnsPagedLogs() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(java.util.Optional.of(adminUser));
        Page<AuditLogResponse> page = new PageImpl<>(List.of(sampleLog(99L)),
                PageRequest.of(0, 20), 1);
        when(auditLogService.list(eq(null), eq(null), eq(null), eq(null), eq(null), eq(null), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/audit-logs").principal(adminAuth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(99L))
                .andExpect(jsonPath("$.content[0].actorEmail").value("admin@example.com"))
                .andExpect(jsonPath("$.content[0].action").value("DELETE"))
                .andExpect(jsonPath("$.content[0].entityName").value("Order"))
                .andExpect(jsonPath("$.content[0].entityId").value(50L));
    }

    @Test
    void listAcceptsFilterParams() throws Exception {
        when(userRepository.findByEmailIgnoreCase("admin@example.com"))
                .thenReturn(java.util.Optional.of(adminUser));
        Page<AuditLogResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(auditLogService.list(eq(1L), eq("Order"), eq(50L), eq(Action.DELETE),
                any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/audit-logs")
                .principal(adminAuth)
                .param("actorId", "1")
                .param("entity", "Order")
                .param("entityId", "50")
                .param("action", "DELETE")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk());
    }
}
