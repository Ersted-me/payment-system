package com.ersted.individualsapi.exception;

public class InvalidScopeException extends KeycloakClientException {

    public InvalidScopeException(String invalidScopeRequested) {
        super(invalidScopeRequested);
    }

}
