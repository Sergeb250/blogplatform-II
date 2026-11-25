package com.amali.blogPlatform.graphql;

import com.amali.blogPlatform.dto.request.CreateReviewRequest;
import com.amali.blogPlatform.dto.request.UpdateReviewRequest;
import com.amali.blogPlatform.dto.response.ReviewResponse;
import com.amali.blogPlatform.service.ReviewService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

/** GraphQL queries and mutations for reviews; uses the same {@link ReviewService} as REST. */
@Controller
public class ReviewGraphQLController {

    private final ReviewService reviewService;
    private final RequestValidator validator;

    public ReviewGraphQLController(ReviewService reviewService, RequestValidator validator) {
        this.reviewService = reviewService;
        this.validator = validator;
    }

    @QueryMapping
    public List<ReviewResponse> reviews(@Argument Long postId) {
        return reviewService.getReviewsByPost(validator.requireId(postId, "postId"));
    }

    @QueryMapping
    public ReviewResponse review(@Argument Long id) {
        return reviewService.getReviewById(validator.requireId(id, "id"));
    }

    @MutationMapping
    public ReviewResponse createReview(@Argument Long postId, @Argument Long userId, @Argument Integer rating,
                                       @Argument String reviewText) {
        return reviewService.createReview(validator.requireId(postId, "postId"), validator.validate(new CreateReviewRequest(userId, rating, reviewText)));
    }

    @MutationMapping
    public ReviewResponse updateReview(@Argument Long id, @Argument Integer rating, @Argument String reviewText) {
        return reviewService.updateReview(validator.requireId(id, "id"), validator.validate(new UpdateReviewRequest(rating, reviewText)));
    }

    @MutationMapping
    public boolean deleteReview(@Argument Long id) {
        reviewService.deleteReview(validator.requireId(id, "id"));
        return true;
    }
}
