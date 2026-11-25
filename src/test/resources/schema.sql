-- Test schema for H2 (MySQL mode): the same tables, columns and constraints as
-- src/main/resources/db/schema-mysql.sql, written in syntax H2 accepts.

CREATE TABLE users (
    user_id       BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('ADMIN', 'AUTHOR', 'READER') NOT NULL DEFAULT 'READER',
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at    TIMESTAMP    NULL DEFAULT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_username CHECK (CHAR_LENGTH(username) >= 3)
);

CREATE TABLE posts (
    post_id    BIGINT       NOT NULL AUTO_INCREMENT,
    author_id  BIGINT       NOT NULL,
    title      VARCHAR(200) NOT NULL,
    content    TEXT         NOT NULL,
    status     ENUM('DRAFT', 'PUBLISHED') NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP    NULL DEFAULT NULL,
    PRIMARY KEY (post_id),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT uq_posts_author_title UNIQUE (author_id, title)
);
CREATE INDEX idx_posts_status_created ON posts (status, created_at);
CREATE INDEX idx_posts_author_created ON posts (author_id, created_at);
CREATE INDEX idx_posts_title ON posts (title);
CREATE INDEX idx_posts_created_at ON posts (created_at);

CREATE TABLE tags (
    tag_id BIGINT      NOT NULL AUTO_INCREMENT,
    name   VARCHAR(50) NOT NULL,
    deleted_at TIMESTAMP   NULL DEFAULT NULL,
    PRIMARY KEY (tag_id),
    CONSTRAINT uq_tags_name UNIQUE (name)
);

CREATE TABLE post_tags (
    post_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id),
    CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_post_tags_tag FOREIGN KEY (tag_id) REFERENCES tags (tag_id) ON DELETE CASCADE
);

CREATE TABLE comments (
    comment_id BIGINT    NOT NULL AUTO_INCREMENT,
    post_id    BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    content    TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (comment_id),
    CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE TABLE reviews (
    review_id   BIGINT    NOT NULL AUTO_INCREMENT,
    post_id     BIGINT    NOT NULL,
    user_id     BIGINT    NOT NULL,
    rating      TINYINT   NOT NULL,
    review_text TEXT      NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (review_id),
    CONSTRAINT fk_reviews_post FOREIGN KEY (post_id) REFERENCES posts (post_id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT uq_reviews_post_user UNIQUE (post_id, user_id),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);
