package com.ersted.exception;

public class InvalidScopeException extends KeycloakClientException {

    public InvalidScopeException(String invalidScopeRequested) {
        super(invalidScopeRequested);
    }

}
