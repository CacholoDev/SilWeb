package com.silvaldeweb.exception.cart;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class CartItemNotFoundException extends BusinessException {

    public CartItemNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Cart item not found", "Cart item with id " + id + " was not found.");
    }
}
