package com.silvaldeweb.exception.order;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class OrderNotFoundException extends BusinessException {

    public OrderNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Order not found", "Order with id " + id + " was not found.");
    }
}
