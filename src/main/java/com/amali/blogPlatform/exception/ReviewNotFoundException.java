package com.amali.blogPlatform.exception;

/** No review has the requested id. */
public class ReviewNotFoundException extends ResourceNotFoundException {

    public ReviewNotFoundException(long id) {
        super("Review", id);
    }
}
