package com.silvaldeweb.exception.user;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "User not found", "User with id " + id + " was not found.");
    }
}
