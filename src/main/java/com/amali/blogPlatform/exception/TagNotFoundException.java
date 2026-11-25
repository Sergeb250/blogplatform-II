package com.amali.blogPlatform.exception;

/** No tag has the requested id. */
public class TagNotFoundException extends ResourceNotFoundException {

    public TagNotFoundException(long id) {
        super("Tag", id);
    }
}
