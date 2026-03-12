package com.ersted.individualsapi.exception;

public class KeycloakClientBadRequestException extends KeycloakClientException {

    public KeycloakClientBadRequestException(String errorDescription) {
        super(errorDescription);
    }

}
