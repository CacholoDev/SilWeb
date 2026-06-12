package com.silvaldeweb.controller.audit;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.silvaldeweb.config.AuthUtils;
import com.silvaldeweb.dto.audit.AuditLogResponse;
import com.silvaldeweb.model.audit.Action;
import com.silvaldeweb.model.user.User;
import com.silvaldeweb.repository.user.UserRepository;
import com.silvaldeweb.service.audit.AuditLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private static final Logger log = LoggerFactory.getLogger(AuditLogController.class);

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @GetMapping
    public Page<AuditLogResponse> list(@RequestParam(required = false) Long actorId,
                                       @RequestParam(required = false) String entity,
                                       @RequestParam(required = false) Long entityId,
                                       @RequestParam(required = false) Action action,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       Authentication authentication) {
        User user = AuthUtils.currentUser(authentication, userRepository);
        log.info("GET /api/audit-logs userId={} actorId={} entity={}#{} action={} from={} to={} page={} size={}",
                user.getId(), actorId, entity, entityId, action, from, to, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogService.list(actorId, entity, entityId, action, from, to, pageable);
    }
}
