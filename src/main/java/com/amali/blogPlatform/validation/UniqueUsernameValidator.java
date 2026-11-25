package com.amali.blogPlatform.validation;

import com.amali.blogPlatform.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Checks {@link UniqueUsername}. Spring creates the validator, so the repository is injected through
 * the constructor like in any other bean.
 */
public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String> {

    private final UserRepository userRepository;

    public UniqueUsernameValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null || username.isBlank()) {
            return true;              // "is it empty?" is the job of @NotBlank, not of this rule
        }
        return !userRepository.existsByUsername(username);
    }
}
