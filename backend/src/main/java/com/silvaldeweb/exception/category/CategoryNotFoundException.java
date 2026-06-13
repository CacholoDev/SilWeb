package com.silvaldeweb.exception.category;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class CategoryNotFoundException extends BusinessException {

    public CategoryNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Category not found", "Category with id " + id + " was not found.");
    }
}
