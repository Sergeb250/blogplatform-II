package com.amali.blogPlatform.util;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * Hashes and checks passwords with BCrypt (the same library the previous project used).
 * Only the hash is stored in {@code users.password_hash}; the plain password is never saved.
 */
@Component
public class PasswordHasher {

    public String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt());
    }

    /** True if the plain password belongs to the stored hash. A hash that is not BCrypt never matches. */
    public boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || !storedHash.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
            // a damaged or fake hash in the database: treat it as "does not match", never as a server error
            return false;
        }
    }
}
