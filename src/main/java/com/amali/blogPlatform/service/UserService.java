package com.amali.blogPlatform.service;

import com.amali.blogPlatform.dto.request.CreateUserRequest;
import com.amali.blogPlatform.dto.request.UpdateUserRequest;
import com.amali.blogPlatform.dto.response.UserResponse;
import com.amali.blogPlatform.exception.DuplicateEmailException;
import com.amali.blogPlatform.exception.DuplicateUsernameException;
import com.amali.blogPlatform.exception.UserNotFoundException;
import com.amali.blogPlatform.model.Role;
import com.amali.blogPlatform.model.User;
import com.amali.blogPlatform.repository.CommentRepository;
import com.amali.blogPlatform.repository.PostRepository;
import com.amali.blogPlatform.repository.ReviewRepository;
import com.amali.blogPlatform.repository.UserRepository;
import com.amali.blogPlatform.util.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Business logic for users. Controllers call this class, never the repositories directly. */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordHasher passwordHasher;

    public UserService(UserRepository userRepository, PostRepository postRepository,
                       CommentRepository commentRepository, ReviewRepository reviewRepository,
                       PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.commentRepository = commentRepository;
        this.reviewRepository = reviewRepository;
        this.passwordHasher = passwordHasher;
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    /** The users with these ids, by id (missing ids are simply not in the map). */
    public Map<Long, UserResponse> getUsersByIds(Collection<Long> ids) {
        Map<Long, UserResponse> usersById = new HashMap<>();
        for (User user : userRepository.findByIds(ids)) {
            usersById.put(user.id(), UserResponse.from(user));
        }
        return usersById;
    }

    public UserResponse getUserById(long id) {
        return UserResponse.from(findUser(id));
    }

    /** Creates a user. Username and email must be free; the password is stored as a BCrypt hash. */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUsernameException(request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }
        Role role = request.role() != null ? request.role() : Role.READER;
        String passwordHash = passwordHasher.hash(request.password());

        User newUser = new User(null, request.username(), request.email(), passwordHash, role, null);
        long id = userRepository.insert(newUser);
        return getUserById(id);
    }

    /** Updates a user. An empty password keeps the old one; a null role keeps the old role. */
    @Transactional
    public UserResponse updateUser(long id, UpdateUserRequest request) {
        User existing = findUser(id);

        if (isUsernameTakenByAnotherUser(request.username(), id)) {
            throw new DuplicateUsernameException(request.username());
        }
        if (isEmailTakenByAnotherUser(request.email(), id)) {
            throw new DuplicateEmailException(request.email());
        }
        String passwordHash = hasText(request.password())
                ? passwordHasher.hash(request.password())
                : existing.passwordHash();
        Role role = request.role() != null ? request.role() : existing.role();

        User changed = new User(id, request.username(), request.email(), passwordHash, role, existing.createdAt());
        userRepository.update(changed);
        return getUserById(id);
    }

    /**
     * Soft-deletes a user: the row stays in the table with a {@code deleted_at} date and is hidden from
     * the API. Everything that belongs to the user is soft-deleted with it, in ONE transaction:
     * their comments and reviews, the comments and reviews on their posts, and their posts.
     * (A hard DELETE did this with ON DELETE CASCADE; an UPDATE does not trigger it, so we do it here.)
     */
    @Transactional
    public void deleteUser(long id) {
        if (userRepository.softDeleteById(id) == 0) {
            throw new UserNotFoundException(id);
        }
        commentRepository.softDeleteByUserId(id);
        reviewRepository.softDeleteByUserId(id);
        postRepository.softDeleteByAuthorId(id);
    }

    private User findUser(long id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    /** Also checks deleted users: their usernames stay reserved (the row and its UNIQUE value still exist). */
    private boolean isUsernameTakenByAnotherUser(String username, long userId) {
        return userRepository.isUsernameTakenByAnother(username, userId);
    }

    /** Also checks deleted users, like {@link #isUsernameTakenByAnotherUser}. */
    private boolean isEmailTakenByAnotherUser(String email, long userId) {
        return userRepository.isEmailTakenByAnother(email, userId);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
