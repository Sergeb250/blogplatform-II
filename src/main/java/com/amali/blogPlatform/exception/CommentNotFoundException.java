package com.amali.blogPlatform.exception;

/** No comment has the requested id. */
public class CommentNotFoundException extends ResourceNotFoundException {

    public CommentNotFoundException(long id) {
        super("Comment", id);
    }
}
