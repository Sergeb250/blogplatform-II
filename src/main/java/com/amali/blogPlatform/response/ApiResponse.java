package com.amali.blogPlatform.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The standard body of every REST response: the HTTP status code, a human-readable message and
 * the payload (or null / the field errors when a request is rejected).
 *
 * @param <T> type of the payload
 */
@Schema(description = "Standard response: HTTP status code, message and data")
public record ApiResponse<T>(int status, String message, T data) {

    public static <T> ApiResponse<T> of(int status, String message, T data) {
        return new ApiResponse<>(status, message, data);
    }

    /** 200 OK. */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /** 201 Created. */
    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(201, message, data);
    }
}
