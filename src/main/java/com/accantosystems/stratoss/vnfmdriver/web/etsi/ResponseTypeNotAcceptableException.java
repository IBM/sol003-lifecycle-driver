package com.accantosystems.stratoss.vnfmdriver.web.etsi;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_ACCEPTABLE)
public class ResponseTypeNotAcceptableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResponseTypeNotAcceptableException(String message) {
        super(message);
    }

    public ResponseTypeNotAcceptableException(String message, Throwable cause) {
        super(message, cause);
    }

}
