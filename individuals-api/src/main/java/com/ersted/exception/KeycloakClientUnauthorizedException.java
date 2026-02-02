package com.ersted.exception;

public class KeycloakClientUnauthorizedException extends KeycloakClientException {

    public KeycloakClientUnauthorizedException(String authenticationFailed) {
        super(authenticationFailed);
    }

}
