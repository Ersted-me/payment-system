package com.ersted.individualsapi.exception;

public class KeycloakClientForbiddenException extends KeycloakClientException {

    public KeycloakClientForbiddenException(String accessDenied) {
        super(accessDenied);
    }

}
