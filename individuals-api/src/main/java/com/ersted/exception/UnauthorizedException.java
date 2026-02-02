package com.ersted.exception;

public class UnauthorizedException extends KeycloakClientException {

    public UnauthorizedException(String clientNotAuthorized) {
        super(clientNotAuthorized);
    }

}
