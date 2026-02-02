package com.ersted.exception;

public class KeycloakClientConflictException extends KeycloakClientException {

    public KeycloakClientConflictException(String resourceAlreadyExists) {
        super(resourceAlreadyExists);
    }

}
