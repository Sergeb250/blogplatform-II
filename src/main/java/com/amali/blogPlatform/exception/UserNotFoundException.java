package com.amali.blogPlatform.exception;

/** No user has the requested id. */
public class UserNotFoundException extends ResourceNotFoundException {

    public UserNotFoundException(long id) {
        super("User", id);
    }
}
