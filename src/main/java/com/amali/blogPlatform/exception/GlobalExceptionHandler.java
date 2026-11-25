package com.amali.blogPlatform.exception;

import com.amali.blogPlatform.response.ApiResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The ONE place that turns exceptions into HTTP responses, for every controller. Every error has the
 * same shape as a success: {@code {"status": ..., "message": ..., "data": ...}}. For validation errors
 * {@code data} holds one message per invalid field, e.g. {@code {"title": "Title is required"}}.
 * Controllers and services therefore need no try/catch.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** The custom "already exists" rules; when one of them fails the answer is 409, not 400. */
    private static final Set<String> UNIQUE_RULES = Set.of("UniqueUsername", "UniqueEmail", "UniquePostTitle");

    // ---------------------------------------------------------------- 400 Bad Request

    /** A request body broke a Bean Validation rule (@NotBlank, @Email, @Size, @UniqueEmail, ...). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleInvalidBody(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        boolean duplicate = false;
        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());   // first message per field
            if (UNIQUE_RULES.contains(error.getCode())) {
                duplicate = true;
            }
        }
        if (duplicate) {
            return error(HttpStatus.CONFLICT, "Duplicate value", fieldErrors);
        }
        return error(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    /** A query parameter broke a rule (e.g. size=0 against @Min(1)). */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleInvalidParameters(HandlerMethodValidationException e) {
        Map<String, String> parameterErrors = new LinkedHashMap<>();
        e.getParameterValidationResults().forEach(result -> {
            String name = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(err -> parameterErrors.putIfAbsent(name, err.getDefaultMessage()));
        });
        return error(HttpStatus.BAD_REQUEST, "Validation failed", parameterErrors);
    }

    /** Validation of a service method argument or GraphQL argument failed. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(ConstraintViolationException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> errors.putIfAbsent(v.getPropertyPath().toString(), v.getMessage()));
        return error(HttpStatus.BAD_REQUEST, "Validation failed", errors);
    }

    /** A value not allowed by the service, e.g. sortBy=password_hash. */
    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidParameter(InvalidParameterException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage(), null);
    }

    /** The body is not valid JSON, or a value has the wrong type (e.g. "status": "LATER"). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableBody(HttpMessageNotReadableException e) {
        // Valid JSON, but one value has the wrong type, e.g. "role": "KING" or "authorId": "abc".
        if (e.getCause() instanceof InvalidFormatException wrongValue && !wrongValue.getPath().isEmpty()) {
            String field = wrongValue.getPath().get(wrongValue.getPath().size() - 1).getFieldName();
            Class<?> type = wrongValue.getTargetType();
            String allowed = type.isEnum() ? " (allowed: " + Arrays.toString(type.getEnumConstants()) + ")" : "";
            return error(HttpStatus.BAD_REQUEST,
                    "Invalid value '" + wrongValue.getValue() + "' for field '" + field + "'" + allowed, null);
        }
        return error(HttpStatus.BAD_REQUEST, "Request body is missing or is not valid JSON", null);
    }

    /** A path or query value has the wrong type, e.g. /api/posts/abc or ?status=LATER. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value '" + e.getValue() + "' for parameter '" + e.getName() + "'", null);
    }

    /** A required query parameter is missing, e.g. /api/posts/search without keyword. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        return error(HttpStatus.BAD_REQUEST, "Missing required parameter '" + e.getParameterName() + "'", null);
    }

    /** The database refused the row (NOT NULL, CHECK or FOREIGN KEY constraint). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException e) {
        if (e instanceof DuplicateKeyException) {
            // the UNIQUE constraint caught a duplicate the service check missed (two requests at the same moment)
            return error(HttpStatus.CONFLICT, "A record with the same unique value already exists", null);
        }
        log.warn("Data integrity violation: {}", e.getMostSpecificCause().getMessage());
        return error(HttpStatus.BAD_REQUEST, "The data breaks a database rule", null);
    }

    // ---------------------------------------------------------------- 401, 404, 405, 409

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidCredentials(InvalidCredentialsException e) {
        return error(HttpStatus.UNAUTHORIZED, e.getMessage(), null);
    }

    /** A record asked for by id does not exist (User/Post/Comment/Tag/ReviewNotFoundException). */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    /** The URL does not exist at all, e.g. /api/postz. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownUrl(NoResourceFoundException e) {
        return error(HttpStatus.NOT_FOUND, "No endpoint " + e.getHttpMethod() + " /" + e.getResourcePath(), null);
    }

    /** e.g. PATCH /api/posts/1, which this API does not offer. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, "Method " + e.getMethod() + " is not supported here", null);
    }

    /** The service found a duplicate (username, email, tag name, post title, second review). */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleDuplicate(DuplicateResourceException e) {
        Map<String, String> fieldError = e.getField() == null ? null : Map.of(e.getField(), e.getMessage());
        return error(HttpStatus.CONFLICT, e.getMessage(), fieldError);
    }

    // ---------------------------------------------------------------- 500

    /**
     * Anything else is a bug or an outage. The details go to the log for developers; the client only
     * gets a general message, so no internal information (SQL, stack traces) leaks out.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        if (e instanceof ErrorResponse springError) {
            // a Spring MVC error that already knows its status (e.g. 415 Unsupported Media Type)
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            return error(status, status.getReasonPhrase(), null);
        }
        log.error("Unexpected error", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", null);
    }

    private static <T> ResponseEntity<ApiResponse<T>> error(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status).body(ApiResponse.of(status.value(), message, data));
    }
}
