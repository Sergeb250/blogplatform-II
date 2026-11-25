package com.amali.blogPlatform.exception;

/** A query parameter has a value that is not allowed (e.g. sortBy=password). Answered with 400. */
public class InvalidParameterException extends RuntimeException {

    public InvalidParameterException(String message) {
        super(message);
    }
}
