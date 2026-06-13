package com.silvaldeweb.exception.cart;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class CartNotFoundException extends BusinessException {

    public CartNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Cart not found", "Cart with id " + id + " was not found.");
    }
}
