package com.amali.blogPlatform.controller;

import com.amali.blogPlatform.dto.request.CreateCommentRequest;
import com.amali.blogPlatform.dto.request.UpdateCommentRequest;
import com.amali.blogPlatform.dto.response.CommentResponse;
import com.amali.blogPlatform.response.ApiResponse;
import com.amali.blogPlatform.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** REST endpoints for comments: nested under a post to add and list, flat by id for one comment. */
@RestController
@RequestMapping("/api")
@Tag(name = "Comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "Add a comment to a post")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Comment added successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing user id or content"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post or user not found")
    })
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(@PathVariable long postId,
                                                                   @Valid @RequestBody CreateCommentRequest request) {
        CommentResponse created = commentService.addComment(postId, request);
        return ResponseEntity.created(URI.create("/api/comments/" + created.id()))
                .body(ApiResponse.created("Comment added successfully", created));
    }

    @Operation(summary = "List the comments of a post", description = "Oldest first.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The comments"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found")
    })
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommentResponse>> getCommentsByPost(@PathVariable long postId) {
        return ApiResponse.ok("Comments retrieved successfully", commentService.getCommentsByPost(postId));
    }

    @Operation(summary = "List comments", description = "All comments, newest first. Optional filters: userId (written by this user) and postAuthorId (on the posts of this author).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The comments")
    @GetMapping("/comments")
    public ApiResponse<List<CommentResponse>> getComments(@RequestParam(required = false) Long userId,
                                                          @RequestParam(required = false) Long postAuthorId) {
        return ApiResponse.ok("Comments retrieved successfully", commentService.getComments(userId, postAuthorId));
    }

    @Operation(summary = "Get one comment")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The comment"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment not found")
    })
    @GetMapping("/comments/{id}")
    public ApiResponse<CommentResponse> getComment(@PathVariable long id) {
        return ApiResponse.ok("Comment retrieved successfully", commentService.getCommentById(id));
    }

    @Operation(summary = "Edit a comment", description = "Only the text of a comment can change.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Comment updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing content"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment not found")
    })
    @PutMapping("/comments/{id}")
    public ApiResponse<CommentResponse> updateComment(@PathVariable long id,
                                                      @Valid @RequestBody UpdateCommentRequest request) {
        return ApiResponse.ok("Comment updated successfully", commentService.updateComment(id, request));
    }

    @Operation(summary = "Delete a comment")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Comment deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Comment not found")
    })
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
