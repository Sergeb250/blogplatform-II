package com.amali.blogPlatform.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Custom Bean Validation rule: no other user may already have this email. Checked by {@link UniqueEmailValidator}. */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueEmailValidator.class)
public @interface UniqueEmail {

    String message() default "Email '${validatedValue}' is already registered";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
