package com.silvaldeweb.exception.address;

import org.springframework.http.HttpStatus;

import com.silvaldeweb.exception.BusinessException;

public class AddressNotFoundException extends BusinessException {

    public AddressNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Address not found", "Address with id " + id + " was not found.");
    }
}
