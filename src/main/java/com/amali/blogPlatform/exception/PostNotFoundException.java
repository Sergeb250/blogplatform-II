package com.amali.blogPlatform.exception;

/** No post has the requested id. */
public class PostNotFoundException extends ResourceNotFoundException {

    public PostNotFoundException(long id) {
        super("Post", id);
    }
}
