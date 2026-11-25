package com.amali.blogPlatform.graphql;

import com.amali.blogPlatform.dto.request.CreateCommentRequest;
import com.amali.blogPlatform.dto.request.UpdateCommentRequest;
import com.amali.blogPlatform.dto.response.CommentResponse;
import com.amali.blogPlatform.service.CommentService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/** GraphQL queries and mutations for comments; uses the same {@link CommentService} as REST. */
@Controller
public class CommentGraphQLController {

    private final CommentService commentService;
    private final RequestValidator validator;

    public CommentGraphQLController(CommentService commentService, RequestValidator validator) {
        this.commentService = commentService;
        this.validator = validator;
    }

    @QueryMapping
    public List<CommentResponse> comments(@Argument Long postId) {
        return commentService.getCommentsByPost(validator.requireId(postId, "postId"));
    }

    @QueryMapping
    public CommentResponse comment(@Argument Long id) {
        return commentService.getCommentById(validator.requireId(id, "id"));
    }

    @MutationMapping
    public CommentResponse addComment(@Argument Long postId, @Argument Long userId, @Argument String content) {
        return commentService.addComment(validator.requireId(postId, "postId"), validator.validate(new CreateCommentRequest(userId, content)));
    }

    @MutationMapping
    public CommentResponse updateComment(@Argument Long id, @Argument String content) {
        return commentService.updateComment(validator.requireId(id, "id"), validator.validate(new UpdateCommentRequest(content)));
    }

    @MutationMapping
    public boolean deleteComment(@Argument Long id) {
        commentService.deleteComment(validator.requireId(id, "id"));
        return true;
    }
}
