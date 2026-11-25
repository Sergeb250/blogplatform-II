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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit test: no Spring, no database. The repository and the hasher are Mockito mocks. */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private PostRepository postRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ReviewRepository reviewRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, postRepository, commentRepository, reviewRepository,
                passwordHasher);   // plain constructor injection
    }

    @Test
    void createUserHashesPasswordAndDefaultsRoleToReader() {
        when(passwordHasher.hash("secret123")).thenReturn("HASHED");
        when(userRepository.insert(any())).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(
                new User(7L, "carol", "carol@blog.com", "HASHED", Role.READER, LocalDateTime.now())));

        UserResponse created = userService.createUser(
                new CreateUserRequest("carol", "carol@blog.com", "secret123", null));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).insert(saved.capture());
        assertThat(saved.getValue().passwordHash()).isEqualTo("HASHED");   // never the plain password
        assertThat(saved.getValue().role()).isEqualTo(Role.READER);
        assertThat(created.id()).isEqualTo(7L);
    }

    @Test
    void createUserWithTakenUsernameIsRejected() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
                new CreateUserRequest("alice", "new@blog.com", "secret123", null)))
                .isInstanceOf(DuplicateUsernameException.class);
        verify(userRepository, never()).insert(any());
    }

    @Test
    void createUserWithTakenEmailIsRejected() {
        when(userRepository.existsByEmail("alice@blog.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(
                new CreateUserRequest("newname", "alice@blog.com", "secret123", null)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void updateWithoutPasswordKeepsOldHash() {
        User existing = new User(2L, "alice", "alice@blog.com", "OLD_HASH", Role.AUTHOR, LocalDateTime.now());
        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));
        when(userRepository.isUsernameTakenByAnother("alice2", 2L)).thenReturn(false);
        when(userRepository.isEmailTakenByAnother("alice@blog.com", 2L)).thenReturn(false);   // her own email is fine

        userService.updateUser(2L, new UpdateUserRequest("alice2", "alice@blog.com", "", null));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).update(saved.capture());
        assertThat(saved.getValue().passwordHash()).isEqualTo("OLD_HASH");
        assertThat(saved.getValue().role()).isEqualTo(Role.AUTHOR);
        verify(passwordHasher, never()).hash(any());
    }

    @Test
    void updateToAnotherUsersEmailIsRejected() {
        User alice = new User(2L, "alice", "alice@blog.com", "H", Role.AUTHOR, null);
        when(userRepository.findById(2L)).thenReturn(Optional.of(alice));
        when(userRepository.isUsernameTakenByAnother("alice", 2L)).thenReturn(false);
        when(userRepository.isEmailTakenByAnother("bob@blog.com", 2L)).thenReturn(true);   // bob has it

        assertThatThrownBy(() -> userService.updateUser(2L,
                new UpdateUserRequest("alice", "bob@blog.com", null, null)))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void deleteMissingUserIsNotFound() {
        when(userRepository.softDeleteById(99L)).thenReturn(0);

        assertThatThrownBy(() -> userService.deleteUser(99L))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id 99 not found");
    }
}
