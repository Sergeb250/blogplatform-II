package com.amali.blogPlatform.service;

import com.amali.blogPlatform.dto.request.LoginRequest;
import com.amali.blogPlatform.dto.response.UserResponse;
import com.amali.blogPlatform.exception.InvalidCredentialsException;
import com.amali.blogPlatform.model.User;
import com.amali.blogPlatform.repository.UserRepository;
import com.amali.blogPlatform.util.PasswordHasher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks a username (or email) and password.
 * It is kept apart from {@link UserService} because signing in is a different job than managing users.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    /**
     * The user, if the password matches; otherwise 401. The "username" field may also hold the email
     * address. The message never says which part was wrong, so it does not reveal which accounts exist.
     */
    public UserResponse login(LoginRequest request) {
        String login = request.username().trim();
        User user = userRepository.findByUsername(login)
                .or(() -> userRepository.findByEmail(login))          // not a username? try it as an email
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordHasher.matches(request.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return UserResponse.from(user);
    }
}
