package com.silvaldeweb.exception.category;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class CategoryAlreadyExistsException extends BusinessException {

    public CategoryAlreadyExistsException(String name) {
        super(HttpStatus.CONFLICT, "Category conflict", "Category with name '" + name + "' already exists.");
    }
}
