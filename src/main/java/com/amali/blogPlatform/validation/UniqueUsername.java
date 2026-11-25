package com.amali.blogPlatform.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom Bean Validation rule: no other user may already have this username.
 * The check itself is in {@link UniqueUsernameValidator}, which asks the database.
 * <p>
 * Use it on the username of a NEW user ({@code CreateUserRequest}). When a user is edited, their own
 * username already exists, so the update rule lives in {@code UserService} instead.
 */
@Target(ElementType.FIELD)                         // can be put on a field (a record component)
@Retention(RetentionPolicy.RUNTIME)                // kept at run time, so the validator can find it
@Constraint(validatedBy = UniqueUsernameValidator.class)
public @interface UniqueUsername {

    /** ${validatedValue} is replaced by the value that was checked. */
    String message() default "Username '${validatedValue}' is already taken";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
