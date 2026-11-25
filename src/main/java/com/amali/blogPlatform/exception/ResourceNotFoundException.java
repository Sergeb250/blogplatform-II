package com.amali.blogPlatform.exception;

/** A record asked for by id does not exist. Answered with 404 by {@link GlobalExceptionHandler}. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resource, long id) {
        super(resource + " with id " + id + " not found");
    }
}
