package com.amali.blogPlatform.controller;

import com.amali.blogPlatform.dto.request.CreateUserRequest;
import com.amali.blogPlatform.dto.request.UpdateUserRequest;
import com.amali.blogPlatform.dto.response.UserResponse;
import com.amali.blogPlatform.response.ApiResponse;
import com.amali.blogPlatform.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** REST endpoints for users. Only HTTP concerns here; the work is done by {@link UserService}. */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create a user", description = "Registers a user. The password is stored as a BCrypt hash; role defaults to READER.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid username, email or password"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username or email already in use")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.createUser(request);
        return ResponseEntity.created(URI.create("/api/users/" + created.id()))
                .body(ApiResponse.created("User created successfully", created));
    }

    @Operation(summary = "List all users")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All users (never with password hashes)")
    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.ok("Users retrieved successfully", userService.getAllUsers());
    }

    @Operation(summary = "Get one user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The user"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUser(@Parameter(description = "User id", example = "1") @PathVariable long id) {
        return ApiResponse.ok("User retrieved successfully", userService.getUserById(id));
    }

    @Operation(summary = "Update a user", description = "Leave password out to keep the old one; leave role out to keep the old role.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Username or email used by another user")
    })
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> updateUser(@PathVariable long id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok("User updated successfully", userService.updateUser(id, request));
    }

    @Operation(summary = "Delete a user", description = "Also deletes the user's posts, comments and reviews (ON DELETE CASCADE).")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "User deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
