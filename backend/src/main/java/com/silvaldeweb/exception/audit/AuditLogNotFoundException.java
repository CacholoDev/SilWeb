package com.silvaldeweb.exception.audit;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class AuditLogNotFoundException extends BusinessException {

    public AuditLogNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Audit log not found", "AuditLog with id " + id + " was not found.");
    }
}
