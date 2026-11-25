-- Schema of the existing bloggingPlatform MySQL database (from the previous project, docs/sql/01_schema.sql).
-- Run once on a new MySQL server: mysql -u root -p < schema-mysql.sql
-- The application does not run this file automatically (spring.sql.init.mode=never).

CREATE DATABASE IF NOT EXISTS bloggingPlatform;
USE bloggingPlatform;

-- -----------------------------------------------------
-- users
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
                       user_id       INT UNSIGNED NOT NULL AUTO_INCREMENT,
                       username      VARCHAR(50)  NOT NULL,
                       email         VARCHAR(255) NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       role          ENUM('ADMIN', 'AUTHOR', 'READER') NOT NULL DEFAULT 'READER',
                       created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       deleted_at DATETIME NULL DEFAULT NULL,   -- soft delete: NULL = active, a date = deleted at that time

                       PRIMARY KEY (user_id),
                       CONSTRAINT uq_users_username UNIQUE (username),
                       CONSTRAINT uq_users_email UNIQUE (email),
                       CONSTRAINT chk_users_username CHECK (CHAR_LENGTH(username) >= 3)
) ;

-- -----------------------------------------------------
-- posts
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS posts (
                       post_id    INT UNSIGNED NOT NULL AUTO_INCREMENT,
                       author_id  INT UNSIGNED NOT NULL,
                       title      VARCHAR(200) NOT NULL,
                       content    TEXT         NOT NULL,
                       status     ENUM('DRAFT', 'PUBLISHED') NOT NULL DEFAULT 'DRAFT',
                       created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                       deleted_at DATETIME NULL DEFAULT NULL,   -- soft delete: NULL = active, a date = deleted at that time

                       PRIMARY KEY (post_id),
                       CONSTRAINT fk_posts_author FOREIGN KEY (author_id)
                           REFERENCES users (user_id) ON DELETE CASCADE,
                       CONSTRAINT uq_posts_author_title UNIQUE (author_id, title),

                       -- Phase 4 indexes (for an existing database: CREATE INDEX ... ON posts (...))
                       INDEX idx_posts_status_created (status, created_at),    -- published posts, newest first (Discover, paging)
                       INDEX idx_posts_author_created (author_id, created_at), -- one author's posts, newest first (My posts)
                       INDEX idx_posts_title (title),                          -- posts in title order (sortBy=title)
                       INDEX idx_posts_created_at (created_at)                 -- all posts, newest first (the default GET /api/posts)
) ;

-- -----------------------------------------------------
-- tags
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS tags (
                      tag_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
                      name   VARCHAR(50)  NOT NULL,
                       deleted_at DATETIME NULL DEFAULT NULL,   -- soft delete: NULL = active, a date = deleted at that time

                      PRIMARY KEY (tag_id),
                      CONSTRAINT uq_tags_name UNIQUE (name)
) ;

-- -----------------------------------------------------
-- post_tags (junction table: posts <-> tags)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS post_tags (
                           post_id INT UNSIGNED NOT NULL,
                           tag_id  INT UNSIGNED NOT NULL,

                           PRIMARY KEY (post_id, tag_id),
                           CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id)
                               REFERENCES posts (post_id) ON DELETE CASCADE,
                           CONSTRAINT fk_post_tags_tag FOREIGN KEY (tag_id)
                               REFERENCES tags (tag_id) ON DELETE CASCADE
) ;

-- -----------------------------------------------------
-- comments
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS comments (
                          comment_id INT UNSIGNED NOT NULL AUTO_INCREMENT,
                          post_id    INT UNSIGNED NOT NULL,
                          user_id    INT UNSIGNED NOT NULL,
                          content    TEXT         NOT NULL,
                          created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       deleted_at DATETIME NULL DEFAULT NULL,   -- soft delete: NULL = active, a date = deleted at that time

                          PRIMARY KEY (comment_id),
                          CONSTRAINT fk_comments_post FOREIGN KEY (post_id)
                              REFERENCES posts (post_id) ON DELETE CASCADE,
                          CONSTRAINT fk_comments_user FOREIGN KEY (user_id)
                              REFERENCES users (user_id) ON DELETE CASCADE
) ;

-- -----------------------------------------------------
-- reviews (one review per user per post)
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS reviews (
                         review_id   INT UNSIGNED     NOT NULL AUTO_INCREMENT,
                         post_id     INT UNSIGNED     NOT NULL,
                         user_id     INT UNSIGNED     NOT NULL,
                         rating      TINYINT UNSIGNED NOT NULL,
                         review_text TEXT             NULL,
                         created_at  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       deleted_at DATETIME NULL DEFAULT NULL,   -- soft delete: NULL = active, a date = deleted at that time

                         PRIMARY KEY (review_id),
                         CONSTRAINT fk_reviews_post FOREIGN KEY (post_id)
                             REFERENCES posts (post_id) ON DELETE CASCADE,
                         CONSTRAINT fk_reviews_user FOREIGN KEY (user_id)
                             REFERENCES users (user_id) ON DELETE CASCADE,
                         CONSTRAINT uq_reviews_post_user UNIQUE (post_id, user_id),
                         CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ;

