package com.amali.blogPlatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.amali.blogPlatform.model.Tag;

/** A tag as the API returns it. */
@Schema(description = "A tag")
public record TagResponse(Long id, String name) {

    public static TagResponse from(Tag tag) {
        return new TagResponse(tag.id(), tag.name());
    }
}
