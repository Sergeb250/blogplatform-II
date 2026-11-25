package com.amali.blogPlatform.service;

import com.amali.blogPlatform.dto.request.CreateCommentRequest;
import com.amali.blogPlatform.dto.request.UpdateCommentRequest;
import com.amali.blogPlatform.dto.response.CommentResponse;
import com.amali.blogPlatform.exception.CommentNotFoundException;
import com.amali.blogPlatform.exception.PostNotFoundException;
import com.amali.blogPlatform.exception.UserNotFoundException;
import com.amali.blogPlatform.model.Comment;
import com.amali.blogPlatform.repository.CommentRepository;
import com.amali.blogPlatform.repository.PostRepository;
import com.amali.blogPlatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Business logic for comments on posts. */
@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository,
                          UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    /** The comments of a post; 404 when the post does not exist (instead of an empty list). */
    public List<CommentResponse> getCommentsByPost(long postId) {
        checkPostExists(postId);
        return commentRepository.findByPostId(postId).stream().map(CommentResponse::from).toList();
    }

    /**
     * All comments, or only those written by {@code userId}, or only those on the posts of
     * {@code postAuthorId} (both optional), newest first.
     */
    public List<CommentResponse> getComments(Long userId, Long postAuthorId) {
        return commentRepository.findAll(userId, postAuthorId).stream().map(CommentResponse::from).toList();
    }

    public CommentResponse getCommentById(long id) {
        return commentRepository.findById(id)
                .map(CommentResponse::from)
                .orElseThrow(() -> new CommentNotFoundException(id));
    }

    /** Adds a comment by an existing user to an existing post. */
    @Transactional
    public CommentResponse addComment(long postId, CreateCommentRequest request) {
        checkPostExists(postId);
        if (!userRepository.existsById(request.userId())) {
            throw new UserNotFoundException(request.userId());
        }
        Comment newComment = new Comment(null, postId, null, request.userId(), null, request.content(), null);
        long id = commentRepository.insert(newComment);
        return getCommentById(id);
    }

    /** Changes the text of a comment. */
    @Transactional
    public CommentResponse updateComment(long id, UpdateCommentRequest request) {
        if (commentRepository.updateContent(id, request.content()) == 0) {
            throw new CommentNotFoundException(id);
        }
        return getCommentById(id);
    }

    /** Soft delete: the comment is hidden but stays in the table with a {@code deleted_at} date. */
    @Transactional
    public void deleteComment(long id) {
        if (commentRepository.softDeleteById(id) == 0) {
            throw new CommentNotFoundException(id);
        }
    }

    private void checkPostExists(long postId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }
    }
}
