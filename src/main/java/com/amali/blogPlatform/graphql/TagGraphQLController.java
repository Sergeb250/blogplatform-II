package com.amali.blogPlatform.graphql;

import com.amali.blogPlatform.dto.request.TagRequest;
import com.amali.blogPlatform.dto.response.TagResponse;
import com.amali.blogPlatform.service.TagService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/** GraphQL queries and mutations for tags; uses the same {@link TagService} as REST. */
@Controller
public class TagGraphQLController {

    private final TagService tagService;
    private final RequestValidator validator;

    public TagGraphQLController(TagService tagService, RequestValidator validator) {
        this.tagService = tagService;
        this.validator = validator;
    }

    @QueryMapping
    public List<TagResponse> tags() {
        return tagService.getAllTags();
    }

    @QueryMapping
    public TagResponse tag(@Argument Long id) {
        return tagService.getTagById(validator.requireId(id, "id"));
    }

    @MutationMapping
    public TagResponse createTag(@Argument String name) {
        return tagService.createTag(validator.validate(new TagRequest(name)));
    }

    @MutationMapping
    public TagResponse updateTag(@Argument Long id, @Argument String name) {
        return tagService.updateTag(validator.requireId(id, "id"), validator.validate(new TagRequest(name)));
    }

    @MutationMapping
    public boolean deleteTag(@Argument Long id) {
        tagService.deleteTag(validator.requireId(id, "id"));
        return true;
    }
}
