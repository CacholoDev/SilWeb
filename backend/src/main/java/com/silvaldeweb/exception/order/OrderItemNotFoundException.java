package com.silvaldeweb.exception.order;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class OrderItemNotFoundException extends BusinessException {

    public OrderItemNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Order item not found", "Order item with id " + id + " was not found.");
    }
}
