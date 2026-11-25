package com.amali.blogPlatform.graphql;

import com.amali.blogPlatform.dto.request.CreateUserRequest;
import com.amali.blogPlatform.dto.request.UpdateUserRequest;
import com.amali.blogPlatform.dto.response.PostResponse;
import com.amali.blogPlatform.dto.response.UserResponse;
import com.amali.blogPlatform.model.PostFilter;
import com.amali.blogPlatform.model.Role;
import com.amali.blogPlatform.service.PostService;
import com.amali.blogPlatform.service.UserService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/** GraphQL queries and mutations for users; uses the same {@link UserService} as REST. */
@Controller
public class UserGraphQLController {

    private final UserService userService;
    private final PostService postService;
    private final RequestValidator validator;

    public UserGraphQLController(UserService userService, PostService postService, RequestValidator validator) {
        this.userService = userService;
        this.postService = postService;
        this.validator = validator;
    }

    @QueryMapping
    public List<UserResponse> users() {
        return userService.getAllUsers();
    }

    @QueryMapping
    public UserResponse user(@Argument Long id) {
        return userService.getUserById(validator.requireId(id, "id"));
    }

    /** {@code User.posts}: the user's newest posts (first page of 100). */
    @SchemaMapping(typeName = "User", field = "posts")
    public List<PostResponse> posts(UserResponse user) {
        PostFilter byAuthor = new PostFilter(user.id(), null, null, null);
        return postService.getPosts(byAuthor, 0, PostService.MAX_PAGE_SIZE, "created_at", "DESC").content();
    }

    @MutationMapping
    public UserResponse createUser(@Argument String username, @Argument String email, @Argument String password,
                                   @Argument Role role) {
        return userService.createUser(validator.validate(new CreateUserRequest(username, email, password, role)));
    }

    @MutationMapping
    public UserResponse updateUser(@Argument Long id, @Argument String username, @Argument String email,
                                   @Argument String password, @Argument Role role) {
        return userService.updateUser(validator.requireId(id, "id"), validator.validate(new UpdateUserRequest(username, email, password, role)));
    }

    @MutationMapping
    public boolean deleteUser(@Argument Long id) {
        userService.deleteUser(validator.requireId(id, "id"));
        return true;
    }
}
