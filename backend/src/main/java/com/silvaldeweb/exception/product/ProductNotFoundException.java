package com.silvaldeweb.exception.product;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class ProductNotFoundException extends BusinessException {

    public ProductNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Product not found", "Product with id " + id + " was not found.");
    }
}
