package com.silvaldeweb.exception.audit;

public class AuditLogNotFoundException extends RuntimeException {

    public AuditLogNotFoundException(Long id) {
        super("AuditLog with id " + id + " was not found.");
    }
}
