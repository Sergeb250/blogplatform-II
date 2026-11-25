package com.amali.blogPlatform.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom Bean Validation rule: no other post may already have this title (two posts can never share
 * a title, whoever wrote them; upper/lower case is ignored, so "Java Tips" equals "java tips").
 * The check itself is in {@link UniquePostTitleValidator}, which asks the database.
 * <p>
 * Use it on the title of a NEW post ({@code CreatePostRequest}). When a post is edited, its own title
 * already exists, so the update rule lives in {@code PostService} instead.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniquePostTitleValidator.class)
public @interface UniquePostTitle {

    String message() default "A post titled '${validatedValue}' already exists";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
