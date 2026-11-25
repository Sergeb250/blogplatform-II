package com.amali.blogPlatform.model;

/**
 * Which posts to return. Every field is optional: null means "do not filter on this".
 *
 * @param authorId only posts of this author
 * @param tag      only posts that have a tag with this name (e.g. "Java")
 * @param status   only DRAFT or only PUBLISHED posts
 * @param keyword  only posts whose title or content contains this text
 */
public record PostFilter(Long authorId, String tag, PostStatus status, String keyword) {

    /** No filter at all: every post. */
    public static PostFilter none() {
        return new PostFilter(null, null, null, null);
    }
}
