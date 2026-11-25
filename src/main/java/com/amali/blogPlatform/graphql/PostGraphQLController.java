package com.amali.blogPlatform.graphql;

import com.amali.blogPlatform.dto.request.CreatePostRequest;
import com.amali.blogPlatform.dto.request.UpdatePostRequest;
import com.amali.blogPlatform.dto.response.CommentResponse;
import com.amali.blogPlatform.dto.response.PageResponse;
import com.amali.blogPlatform.dto.response.PostResponse;
import com.amali.blogPlatform.dto.response.ReviewResponse;
import com.amali.blogPlatform.dto.response.UserResponse;
import com.amali.blogPlatform.model.PostFilter;
import com.amali.blogPlatform.model.PostStatus;
import com.amali.blogPlatform.service.CommentService;
import com.amali.blogPlatform.service.PostService;
import com.amali.blogPlatform.service.ReviewService;
import com.amali.blogPlatform.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GraphQL queries, mutations and field resolvers for posts.
 * <p>
 * It uses the SAME {@link PostService} as the REST {@code PostController}, so there is one place for the
 * business rules and one data path: GraphQL controller → service → repository → JdbcTemplate → MySQL.
 */
@Controller
public class PostGraphQLController {

    private final PostService postService;
    private final UserService userService;
    private final CommentService commentService;
    private final ReviewService reviewService;
    private final RequestValidator validator;

    public PostGraphQLController(PostService postService, UserService userService, CommentService commentService,
                                 ReviewService reviewService, RequestValidator validator) {
        this.postService = postService;
        this.userService = userService;
        this.commentService = commentService;
        this.reviewService = reviewService;
        this.validator = validator;
    }

    // ------------------------------------------------------------------ queries

    /** {@code query { posts { id title } }} — the method name matches the field name in type Query. */
    @QueryMapping
    public List<PostResponse> posts(@Argument int page, @Argument int size, @Argument String sortBy,
                                    @Argument String direction, @Argument Long authorId, @Argument String tag,
                                    @Argument PostStatus status) {
        return postPage(page, size, sortBy, direction, authorId, tag, status).content();
    }

    @QueryMapping
    public PageResponse<PostResponse> postPage(@Argument int page, @Argument int size, @Argument String sortBy,
                                               @Argument String direction, @Argument Long authorId,
                                               @Argument String tag, @Argument PostStatus status) {
        return postService.getPosts(new PostFilter(authorId, tag, status, null), page, size, sortBy, direction);
    }

    /** {@code query { post(id: "1") { title } }}. A missing post gives a NOT_FOUND error. */
    @QueryMapping
    public PostResponse post(@Argument Long id) {
        return postService.getPostById(validator.requireId(id, "id"));
    }

    @QueryMapping
    public PageResponse<PostResponse> searchPosts(@Argument String keyword, @Argument int page, @Argument int size) {
        return postService.searchPosts(keyword, page, size, "created_at", "DESC");
    }

    // ------------------------------------------------------------------ fields of type Post

    /**
     * {@code Post.author}. Without batching, a list of 10 posts would run 10 user queries (the N+1
     * problem). {@code @BatchMapping} collects the posts of the whole response and calls this method
     * ONCE, so all authors are loaded with a single {@code WHERE user_id IN (...)} query.
     */
    @BatchMapping(typeName = "Post", field = "author")
    public Map<PostResponse, UserResponse> author(List<PostResponse> posts) {
        Set<Long> authorIds = posts.stream().map(PostResponse::authorId).collect(Collectors.toSet());
        Map<Long, UserResponse> usersById = userService.getUsersByIds(authorIds);

        Map<PostResponse, UserResponse> authorOfPost = new HashMap<>();
        for (PostResponse post : posts) {
            authorOfPost.put(post, usersById.get(post.authorId()));
        }
        return authorOfPost;
    }

    /** {@code Post.comments}: only loaded when the client asks for this field. */
    @SchemaMapping(typeName = "Post", field = "comments")
    public List<CommentResponse> comments(PostResponse post) {
        return commentService.getCommentsByPost(post.id());
    }

    /** {@code Post.reviews}: only loaded when the client asks for this field. */
    @SchemaMapping(typeName = "Post", field = "reviews")
    public List<ReviewResponse> reviews(PostResponse post) {
        return reviewService.getReviewsByPost(post.id());
    }

    // ------------------------------------------------------------------ mutations

    @MutationMapping
    public PostResponse createPost(@Argument String title, @Argument String content, @Argument Long authorId,
                                   @Argument PostStatus status, @Argument List<Long> tagIds) {
        CreatePostRequest request = validator.validate(new CreatePostRequest(title, content, authorId, status, tagIds));
        return postService.createPost(request);
    }

    @MutationMapping
    public PostResponse updatePost(@Argument Long id, @Argument String title, @Argument String content,
                                   @Argument PostStatus status, @Argument List<Long> tagIds) {
        UpdatePostRequest request = validator.validate(new UpdatePostRequest(title, content, status, tagIds));
        return postService.updatePost(validator.requireId(id, "id"), request);
    }

    /** Returns true when the post was deleted (a missing post gives a NOT_FOUND error). */
    @MutationMapping
    public boolean deletePost(@Argument Long id) {
        postService.deletePost(validator.requireId(id, "id"));
        return true;
    }
}
