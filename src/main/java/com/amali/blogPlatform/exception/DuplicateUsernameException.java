package com.amali.blogPlatform.exception;

/** Another user already has this username. */
public class DuplicateUsernameException extends DuplicateResourceException {

    public DuplicateUsernameException(String username) {
        super("username", "Username '" + username + "' is already taken");
    }
}
