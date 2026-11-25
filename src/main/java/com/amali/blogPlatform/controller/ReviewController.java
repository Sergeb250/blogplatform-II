package com.amali.blogPlatform.controller;

import com.amali.blogPlatform.dto.request.CreateReviewRequest;
import com.amali.blogPlatform.dto.request.UpdateReviewRequest;
import com.amali.blogPlatform.dto.response.ReviewResponse;
import com.amali.blogPlatform.response.ApiResponse;
import com.amali.blogPlatform.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/** REST endpoints for reviews: nested under a post to create and list, flat by id for one review. */
@RestController
@RequestMapping("/api")
@Tag(name = "Reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "Review a post", description = "A rating from 1 to 5 with optional text. A user can review a post only once.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Review created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing user id, or rating not 1-5"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post or user not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "This user already reviewed this post")
    })
    @PostMapping("/posts/{postId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(@PathVariable long postId,
                                                                    @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse created = reviewService.createReview(postId, request);
        return ResponseEntity.created(URI.create("/api/reviews/" + created.id()))
                .body(ApiResponse.created("Review created successfully", created));
    }

    @Operation(summary = "List the reviews of a post", description = "Newest first.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The reviews"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Post not found")
    })
    @GetMapping("/posts/{postId}/reviews")
    public ApiResponse<List<ReviewResponse>> getReviewsByPost(@PathVariable long postId) {
        return ApiResponse.ok("Reviews retrieved successfully", reviewService.getReviewsByPost(postId));
    }

    @Operation(summary = "List reviews", description = "All reviews, newest first. Optional filters: userId (by this reviewer) and postAuthorId (on the posts of this author).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The reviews")
    @GetMapping("/reviews")
    public ApiResponse<List<ReviewResponse>> getReviews(@RequestParam(required = false) Long userId,
                                                        @RequestParam(required = false) Long postAuthorId) {
        return ApiResponse.ok("Reviews retrieved successfully", reviewService.getReviews(userId, postAuthorId));
    }

    @Operation(summary = "Get one review")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "The review"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found")
    })
    @GetMapping("/reviews/{id}")
    public ApiResponse<ReviewResponse> getReview(@PathVariable long id) {
        return ApiResponse.ok("Review retrieved successfully", reviewService.getReviewById(id));
    }

    @Operation(summary = "Change a review", description = "The rating and the text can change.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Review updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Rating not 1-5"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found")
    })
    @PutMapping("/reviews/{id}")
    public ApiResponse<ReviewResponse> updateReview(@PathVariable long id,
                                                    @Valid @RequestBody UpdateReviewRequest request) {
        return ApiResponse.ok("Review updated successfully", reviewService.updateReview(id, request));
    }

    @Operation(summary = "Delete a review")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Review deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Review not found")
    })
    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable long id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
