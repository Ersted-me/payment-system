package com.ersted.individualsapi.exception;

public class InvalidCredentialsException extends KeycloakClientException {

    public InvalidCredentialsException(String invalidUsernameOrPassword) {
        super(invalidUsernameOrPassword);
    }

}
