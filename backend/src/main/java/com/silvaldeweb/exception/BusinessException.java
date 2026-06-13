package com.silvaldeweb.exception;

import java.util.Collections;
import java.util.Map;

import org.springframework.http.HttpStatus;

public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String title;
    private final Map<String, Object> properties;

    protected BusinessException(HttpStatus status, String title, String detail) {
        this(status, title, detail, Collections.emptyMap());
    }

    protected BusinessException(HttpStatus status, String title, String detail, Map<String, Object> properties) {
        super(detail);
        this.status = status;
        this.title = title;
        this.properties = properties == null ? Collections.emptyMap() : Map.copyOf(properties);
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
