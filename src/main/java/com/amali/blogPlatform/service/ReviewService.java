package com.amali.blogPlatform.service;

import com.amali.blogPlatform.dto.request.CreateReviewRequest;
import com.amali.blogPlatform.dto.request.UpdateReviewRequest;
import com.amali.blogPlatform.dto.response.ReviewResponse;
import com.amali.blogPlatform.exception.DuplicateResourceException;
import com.amali.blogPlatform.exception.PostNotFoundException;
import com.amali.blogPlatform.exception.ReviewNotFoundException;
import com.amali.blogPlatform.exception.UserNotFoundException;
import com.amali.blogPlatform.model.Review;
import com.amali.blogPlatform.repository.PostRepository;
import com.amali.blogPlatform.repository.ReviewRepository;
import com.amali.blogPlatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/** Business logic for reviews (ratings) of posts. A user can review a post only once. */
@Service
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, PostRepository postRepository,
                         UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    /** The reviews of a post; 404 when the post does not exist. */
    public List<ReviewResponse> getReviewsByPost(long postId) {
        checkPostExists(postId);
        return reviewRepository.findByPostId(postId).stream().map(ReviewResponse::from).toList();
    }

    /** All reviews, or only those by {@code userId}, or only those on the posts of {@code postAuthorId}. */
    public List<ReviewResponse> getReviews(Long userId, Long postAuthorId) {
        return reviewRepository.findAll(userId, postAuthorId).stream().map(ReviewResponse::from).toList();
    }

    public ReviewResponse getReviewById(long id) {
        return reviewRepository.findById(id)
                .map(ReviewResponse::from)
                .orElseThrow(() -> new ReviewNotFoundException(id));
    }

    @Transactional
    public ReviewResponse createReview(long postId, CreateReviewRequest request) {
        checkPostExists(postId);
        if (!userRepository.existsById(request.userId())) {
            throw new UserNotFoundException(request.userId());
        }
        if (reviewRepository.existsByPostIdAndUserId(postId, request.userId())) {
            throw new DuplicateResourceException("userId",
                    "User " + request.userId() + " has already reviewed post " + postId);
        }
        // The user deleted an earlier review of this post? Bring that row back with the new rating
        // (the table allows only one review per user per post, and the deleted row still holds that place).
        Optional<Long> restoredId = reviewRepository.restoreDeleted(postId, request.userId(),
                request.rating(), request.reviewText());
        if (restoredId.isPresent()) {
            return getReviewById(restoredId.get());
        }
        Review newReview = new Review(null, postId, null, request.userId(), null, request.rating(),
                request.reviewText(), null);
        long id = reviewRepository.insert(newReview);
        return getReviewById(id);
    }

    @Transactional
    public ReviewResponse updateReview(long id, UpdateReviewRequest request) {
        if (reviewRepository.update(id, request.rating(), request.reviewText()) == 0) {
            throw new ReviewNotFoundException(id);
        }
        return getReviewById(id);
    }

    /** Soft delete: the review is hidden but stays in the table with a {@code deleted_at} date. */
    @Transactional
    public void deleteReview(long id) {
        if (reviewRepository.softDeleteById(id) == 0) {
            throw new ReviewNotFoundException(id);
        }
    }

    private void checkPostExists(long postId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }
    }
}
