package com.silvaldeweb.exception.order;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class OrderInvalidStateException extends BusinessException {

    public OrderInvalidStateException(String detail) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "Order invalid state", detail);
    }
}
