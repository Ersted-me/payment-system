package com.ersted.individualsapi.exception;

public class InvalidClientException extends KeycloakClientException {

    public InvalidClientException(String invalidClientCredentials) {
        super(invalidClientCredentials);
    }

}
