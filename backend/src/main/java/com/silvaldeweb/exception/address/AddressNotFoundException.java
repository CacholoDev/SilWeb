package com.silvaldeweb.exception.address;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(Long id) {
        super("Address with id " + id + " was not found.");
    }
}
