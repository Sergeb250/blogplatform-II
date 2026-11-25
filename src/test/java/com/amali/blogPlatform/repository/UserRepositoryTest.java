package com.amali.blogPlatform.repository;

import com.amali.blogPlatform.config.DatabaseConfig;
import com.amali.blogPlatform.model.Role;
import com.amali.blogPlatform.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({DatabaseConfig.class, UserRepository.class})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findAllMapsEveryColumn() {
        assertThat(userRepository.findAll()).hasSize(3);
        User alice = userRepository.findById(2).orElseThrow();
        assertThat(alice.username()).isEqualTo("alice");
        assertThat(alice.email()).isEqualTo("alice@blog.com");
        assertThat(alice.role()).isEqualTo(Role.AUTHOR);
        assertThat(alice.createdAt()).isNotNull();
    }

    @Test
    void existsChecks() {
        assertThat(userRepository.existsByUsername("bob")).isTrue();
        assertThat(userRepository.existsByEmail("bob@blog.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@blog.com")).isFalse();
        assertThat(userRepository.findByUsername("nobody")).isEmpty();
    }

    @Test
    void insertUpdateDelete() {
        long id = userRepository.insert(new User(null, "carol", "carol@blog.com", "hash", Role.READER, null));
        assertThat(userRepository.count()).isEqualTo(4);

        userRepository.update(new User(id, "carol2", "carol2@blog.com", "hash2", Role.AUTHOR, null));
        User updated = userRepository.findById(id).orElseThrow();
        assertThat(updated.username()).isEqualTo("carol2");
        assertThat(updated.role()).isEqualTo(Role.AUTHOR);

        assertThat(userRepository.softDeleteById(id)).isEqualTo(1);
        assertThat(userRepository.existsById(id)).isFalse();
    }

    @Test
    void duplicateEmailIsRejectedByUniqueConstraint() {
        assertThatThrownBy(() -> userRepository.insert(
                new User(null, "another", "alice@blog.com", "hash", Role.READER, null)))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
