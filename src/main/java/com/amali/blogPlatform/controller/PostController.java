package com.amali.blogPlatform.controller;

import com.amali.blogPlatform.dto.request.CreatePostRequest;
import com.amali.blogPlatform.dto.request.UpdatePostRequest;
import com.amali.blogPlatform.dto.response.PageResponse;
import com.amali.blogPlatform.dto.response.PostResponse;
import com.amali.blogPlatform.model.PostFilter;
import com.amali.blogPlatform.model.PostStatus;
import com.amali.blogPlatform.response.ApiResponse;
import com.amali.blogPlatform.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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

/**
 * REST endpoints for posts.
 * <p>
 * Note: our response wrapper is also called {@code ApiResponse}, so Swagger's annotation of the same
 * name is written with its full package name: {@code @io.swagger.v3.oas.annotations.responses.ApiResponse}.
 */
@RestController
@RequestMapping("/api/posts")
@Tag(name = "Posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @Operation(summary = "Create a blog post", description = "Creates a new post for an existing user, with optional tags.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Post created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request (validation failed)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Author or tag not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "This author already has a post with this title")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@Valid @RequestBody CreatePostRequest request) {
        PostResponse created = postService.createPost(request);
        return ResponseEntity.created(URI.create("/api/posts/" + created.id()))
                .body(ApiResponse.created("Post created successfully", created));
    }

    @Operation(summary = "List posts (paged, sorted, filtered)",
            description = "Returns one page of posts. Every parameter is optional and they can be combined, "
                    + "e.g. `?page=0&size=5&sortBy=title&direction=ASC&authorId=3&tag=Java`.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "One page of posts"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid page, size, sortBy, direction or status")
    })
    @GetMapping
    public ApiResponse<PageResponse<PostResponse>> getPosts(
            @Parameter(description = "Page number, starting at 0", example = "0")
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or more") int page,
            @Parameter(description = "Posts per page (1-100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be at least 1") @Max(value = 100, message = "size must be at most 100") int size,
            @Parameter(description = "Sort column: created_at, title or rating", example = "created_at")
            @RequestParam(defaultValue = "created_at") String sortBy,
            @Parameter(description = "ASC or DESC", example = "DESC")
            @RequestParam(defaultValue = "DESC") String direction,
            @Parameter(description = "Only posts of this author", example = "3")
            @RequestParam(required = false) Long authorId,
            @Parameter(description = "Only posts with this tag name", example = "Java")
            @RequestParam(required = false) String tag,
            @Parameter(description = "Only DRAFT or only PUBLISHED posts")
            @RequestParam(required = false) PostStatus status,
            @Parameter(description = "Only posts whose title or content contains this text", example = "Spring")
            @RequestParam(required = false) String keyword) {
        PostFilter filter = new PostFilter(authorId, tag, status, keyword);
        return ApiResponse.ok("Posts retrieved successfully",
                postService.getPosts(filter, page, size, sortBy, direction));
    }

    @Operation(summary = "Search posts by keyword", description = "Finds posts whose title or content contains the keyword (case-insensitive).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "One page of matching posts"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing or empty keyword, or invalid paging")
    })
    @GetMapping("/search")
    public ApiResponse<PageResponse<PostResponse>> searchPosts(
            @Parameter(description = "Text to look for in title and content", example = "Java")
            @RequestParam @NotBlank(message = "keyword must not be empty") String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or more") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be at least 1") @Max(value = 100, message = "size must be at most 100") int size,
            @RequestParam(defaultValue = "created_at") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction) {
        return ApiResponse.ok("Search results retrieved successfully",
                postService.searchPosts(keyword, page, size, sortBy, direction));
    }

    @Operation(summary = "Get one post", description = "Returns the post with its author, tags, comment count and average rating.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The post"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found")
    })
    @GetMapping("/{id}")
    public ApiResponse<PostResponse> getPost(@Parameter(description = "Post id", example = "1") @PathVariable long id) {
        return ApiResponse.ok("Post retrieved successfully", postService.getPostById(id));
    }

    @Operation(summary = "Update a post", description = "Changes title, content, status and (when sent) tags. The author cannot change.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Post updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post or tag not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate title for this author")
    })
    @PutMapping("/{id}")
    public ApiResponse<PostResponse> updatePost(@PathVariable long id, @Valid @RequestBody UpdatePostRequest request) {
        return ApiResponse.ok("Post updated successfully", postService.updatePost(id, request));
    }

    @Operation(summary = "Delete a post", description = "Deletes the post with its comments, reviews and tag links.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Post deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
