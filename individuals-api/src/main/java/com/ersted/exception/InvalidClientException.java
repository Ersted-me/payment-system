package com.ersted.exception;

public class InvalidClientException extends KeycloakClientException {

    public InvalidClientException(String invalidClientCredentials) {
        super(invalidClientCredentials);
    }

}
