package com.amali.blogPlatform.validation;

import com.amali.blogPlatform.repository.PostRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Checks {@link UniquePostTitle}. The repository is injected through the constructor, like in any bean. */
public class UniquePostTitleValidator implements ConstraintValidator<UniquePostTitle, String> {

    private final PostRepository postRepository;

    public UniquePostTitleValidator(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Override
    public boolean isValid(String title, ConstraintValidatorContext context) {
        if (title == null || title.isBlank()) {
            return true;              // "is it empty?" is the job of @NotBlank, not of this rule
        }
        return !postRepository.existsByTitle(title, 0);   // 0 = no post to ignore (it is a new post)
    }
}
