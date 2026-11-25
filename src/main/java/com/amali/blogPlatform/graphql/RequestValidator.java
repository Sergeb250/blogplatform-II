package com.amali.blogPlatform.graphql;

import com.amali.blogPlatform.exception.InvalidParameterException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Runs the same Bean Validation rules as REST ({@code @NotBlank}, {@code @Email}, {@code @UniqueEmail}, ...)
 * on a request DTO built by a GraphQL controller. REST gets this for free with {@code @Valid};
 * GraphQL arguments arrive one by one, so the controller builds the DTO and calls this method.
 */
@Component
public class RequestValidator {

    private final Validator validator;

    public RequestValidator(Validator validator) {
        this.validator = validator;
    }

    /** Returns the request unchanged when it is valid; otherwise throws with every broken rule. */
    public <T> T validate(T request) {
        Set<ConstraintViolation<T>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        return request;
    }

    /**
     * Checks an id argument. The schema says {@code ID!}, but an EMPTY value ({@code id: ""}, or a
     * Postman variable like {@code "{{postId}}"} that was never set) still arrives here as {@code null}.
     * Without this check the service would crash with a NullPointerException (an "unexpected error");
     * with it, the client gets a clear BAD_REQUEST message.
     */
    public long requireId(Long id, String argumentName) {
        if (id == null) {
            throw new InvalidParameterException(argumentName + " is required and must be a number, e.g. 1");
        }
        if (id <= 0) {
            throw new InvalidParameterException(argumentName + " must be a positive number (was " + id + ")");
        }
        return id;
    }
}
