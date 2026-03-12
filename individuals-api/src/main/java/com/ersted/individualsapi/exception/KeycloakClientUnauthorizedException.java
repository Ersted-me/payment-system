package com.ersted.individualsapi.exception;

public class KeycloakClientUnauthorizedException extends KeycloakClientException {

    public KeycloakClientUnauthorizedException(String authenticationFailed) {
        super(authenticationFailed);
    }

}
