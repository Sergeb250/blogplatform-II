package com.amali.blogPlatform.exception;

/** Wrong username or password at login. Answered with 401 Unauthorized. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
