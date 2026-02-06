package com.ersted.exception;

public class KeycloakClientServiceUnavailableException extends KeycloakClientException{

    public KeycloakClientServiceUnavailableException(String keycloakUnavailable) {
        super(keycloakUnavailable);
    }

}
