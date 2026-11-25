package com.amali.blogPlatform.exception;

/**
 * The record would break a unique rule (same username, same tag name, ...). Answered with 409 Conflict.
 * {@code field} names the request field that caused it (e.g. "username"), so a client form can show
 * the message next to that input; it may be null.
 */
public class DuplicateResourceException extends RuntimeException {

    private final String field;

    public DuplicateResourceException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
