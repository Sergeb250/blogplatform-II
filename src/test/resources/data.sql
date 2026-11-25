-- Small, known data set for the tests (ids are predictable because the tables start empty).

INSERT INTO users (username, email, password_hash, role, created_at) VALUES
    ('admin',  'admin@blog.com',  '$2a$10$testhashadmin',  'ADMIN',  '2026-01-01 09:00:00'),   -- id 1
    ('alice',  'alice@blog.com',  '$2a$10$testhashalice',  'AUTHOR', '2026-01-02 09:00:00'),   -- id 2
    ('bob',    'bob@blog.com',    '$2a$10$testhashbob',    'READER', '2026-01-03 09:00:00');   -- id 3

INSERT INTO posts (author_id, title, content, status, created_at, updated_at) VALUES
    (2, 'Getting started with Spring Boot', 'Spring Boot makes Java web development quick.', 'PUBLISHED',
        '2026-02-01 10:00:00', '2026-02-01 10:00:00'),                                          -- id 1
    (2, 'JDBC without JPA', 'JdbcTemplate runs plain SQL with placeholders.', 'PUBLISHED',
        '2026-02-02 10:00:00', '2026-02-02 10:00:00'),                                          -- id 2
    (1, 'Draft notes', 'Not ready to publish yet.', 'DRAFT',
        '2026-02-03 10:00:00', '2026-02-03 10:00:00');                                          -- id 3

INSERT INTO tags (name) VALUES ('Java'), ('Spring'), ('Database');   -- ids 1, 2, 3

INSERT INTO post_tags (post_id, tag_id) VALUES (1, 1), (1, 2), (2, 1), (2, 3);

INSERT INTO comments (post_id, user_id, content, created_at) VALUES
    (1, 3, 'Great introduction, thanks!', '2026-02-05 12:00:00'),
    (1, 1, 'Glad it helps.',              '2026-02-05 13:00:00'),
    (2, 3, 'Clear explanation of RowMapper.', '2026-02-06 12:00:00');

INSERT INTO reviews (post_id, user_id, rating, review_text, created_at) VALUES
    (1, 3, 5, 'Excellent', '2026-02-07 12:00:00'),
    (1, 1, 4, NULL,        '2026-02-07 13:00:00'),
    (2, 3, 3, 'Good',      '2026-02-08 12:00:00');
