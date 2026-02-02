package com.ersted.exception;

public class InvalidCredentialsException extends KeycloakClientException {

    public InvalidCredentialsException(String invalidUsernameOrPassword) {
        super(invalidUsernameOrPassword);
    }

}
