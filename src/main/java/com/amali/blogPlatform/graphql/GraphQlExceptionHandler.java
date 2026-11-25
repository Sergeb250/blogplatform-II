package com.amali.blogPlatform.graphql;

import com.amali.blogPlatform.exception.DuplicateResourceException;
import com.amali.blogPlatform.exception.InvalidCredentialsException;
import com.amali.blogPlatform.exception.InvalidParameterException;
import com.amali.blogPlatform.exception.ResourceNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The GraphQL version of {@code GlobalExceptionHandler}. GraphQL always answers HTTP 200; errors go in
 * the {@code "errors"} list of the response, with a {@code classification} (NOT_FOUND, BAD_REQUEST, ...)
 * and, for validation, one message per field in {@code extensions.fieldErrors}.
 */
@Component
public class GraphQlExceptionHandler extends DataFetcherExceptionResolverAdapter {

    private static final Logger log = LoggerFactory.getLogger(GraphQlExceptionHandler.class);

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof ResourceNotFoundException) {
            return error(env, ErrorType.NOT_FOUND, ex.getMessage(), Map.of());
        }
        if (ex instanceof DuplicateResourceException) {
            return error(env, ErrorType.BAD_REQUEST, ex.getMessage(), Map.of("code", "CONFLICT"));
        }
        if (ex instanceof InvalidParameterException) {
            return error(env, ErrorType.BAD_REQUEST, ex.getMessage(), Map.of());
        }
        if (ex instanceof InvalidCredentialsException) {
            return error(env, ErrorType.UNAUTHORIZED, ex.getMessage(), Map.of());
        }
        if (ex instanceof ConstraintViolationException violations) {
            Map<String, String> fieldErrors = new LinkedHashMap<>();
            violations.getConstraintViolations()
                    .forEach(v -> fieldErrors.putIfAbsent(fieldName(v.getPropertyPath().toString()), v.getMessage()));
            return error(env, ErrorType.BAD_REQUEST, "Validation failed", Map.of("fieldErrors", fieldErrors));
        }
        if (ex instanceof BindException bindError) {
            // An argument could not be converted to its Java type, e.g. post(id: "abc") into a Long.
            Object badValue = bindError.getFieldError() == null ? null : bindError.getFieldError().getRejectedValue();
            return error(env, ErrorType.BAD_REQUEST,
                    "Invalid value '" + badValue + "' for argument '" + argumentHolding(env, badValue) + "'", Map.of());
        }
        log.error("Unexpected GraphQL error", ex);
        return error(env, ErrorType.INTERNAL_ERROR, "An unexpected error occurred", Map.of());
    }

    /** "tagIds[0].&lt;list element&gt;" becomes "tagIds[0]", the same key REST uses. */
    private static String fieldName(String propertyPath) {
        return propertyPath.replace(".<list element>", "");
    }

    /** Finds which argument of the query had the value that could not be converted. */
    private static String argumentHolding(DataFetchingEnvironment env, Object badValue) {
        return env.getArguments().entrySet().stream()
                .filter(argument -> Objects.equals(argument.getValue(), badValue))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("?");
    }

    private static GraphQLError error(DataFetchingEnvironment env, ErrorType type, String message,
                                      Map<String, Object> extensions) {
        return GraphqlErrorBuilder.newError(env)
                .errorType(type)
                .message(message)
                .extensions(extensions)
                .build();
    }
}
