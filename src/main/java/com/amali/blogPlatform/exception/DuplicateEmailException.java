package com.amali.blogPlatform.exception;

/** Another user already has this email. */
public class DuplicateEmailException extends DuplicateResourceException {

    public DuplicateEmailException(String email) {
        super("email", "Email '" + email + "' is already registered");
    }
}
