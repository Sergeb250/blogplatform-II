package com.amali.blogPlatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** A tag name and how many posts have that tag. */
@Schema(description = "A tag and its number of posts")
public record TagCountResponse(String tag, long posts) {
}
