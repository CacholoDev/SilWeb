package com.silvaldeweb.exception.user;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class UserAlreadyExistsException extends BusinessException {

    public UserAlreadyExistsException(String email) {
        super(HttpStatus.CONFLICT, "User conflict", "User with email '" + email + "' already exists.");
    }
}
