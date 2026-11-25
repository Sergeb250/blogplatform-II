package com.amali.blogPlatform.controller;

import com.amali.blogPlatform.dto.request.LoginRequest;
import com.amali.blogPlatform.dto.response.UserResponse;
import com.amali.blogPlatform.response.ApiResponse;
import com.amali.blogPlatform.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sign-in: checks the username (or email) and password and returns the user (no tokens or sessions). */
@RestController
@Tag(name = "Authentication")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 200 with the user when the password is right, 401 otherwise. */
    @Operation(summary = "Sign in", description = "Checks the username and password. 200 with the user, 401 when either is wrong.")
    @PostMapping("/login")
    public ApiResponse<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }
}
