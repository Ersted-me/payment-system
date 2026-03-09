package com.ersted.personservice.exception.handler;

import com.ersted.personservice.exception.NotFoundException;
import com.ersted.personservice.exception.ValidateException;
import com.ersted.personservice.model.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {

        ErrorResponse errorBody = new ErrorResponse();
        errorBody.error(ex.getMessage());
        errorBody.status("NOT_FOUND");

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(errorBody);
    }

    @ExceptionHandler(ValidateException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidateException ex) {

        ErrorResponse errorBody = new ErrorResponse();
        errorBody.error(ex.getMessage());
        errorBody.status("BAD_REQUEST");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorBody);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleValidation(Exception ex) {

        ErrorResponse errorBody = new ErrorResponse();
        errorBody.error(ex.getMessage());
        errorBody.status("INTERNAL_SERVER_ERROR");

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody);
    }

}
