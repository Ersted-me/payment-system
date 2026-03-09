package com.ersted.personservice.exception;

public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }

}
