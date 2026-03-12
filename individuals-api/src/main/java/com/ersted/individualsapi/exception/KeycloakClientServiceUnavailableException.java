package com.ersted.individualsapi.exception;

public class KeycloakClientServiceUnavailableException extends KeycloakClientException{

    public KeycloakClientServiceUnavailableException(String keycloakUnavailable) {
        super(keycloakUnavailable);
    }

}
