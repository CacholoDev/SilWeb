package com.silvaldeweb.exception.order;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class OrderAlreadyExistsException extends BusinessException {

    public OrderAlreadyExistsException(String orderNumber) {
        super(HttpStatus.CONFLICT, "Order conflict", "Order with number '" + orderNumber + "' already exists.");
    }
}
