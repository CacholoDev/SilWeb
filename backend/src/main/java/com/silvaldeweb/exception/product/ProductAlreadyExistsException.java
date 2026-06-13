package com.silvaldeweb.exception.product;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class ProductAlreadyExistsException extends BusinessException {

    public ProductAlreadyExistsException(String sku) {
        super(HttpStatus.CONFLICT, "Product conflict", "Product with sku '" + sku + "' already exists.");
    }
}
