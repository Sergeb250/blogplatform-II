-- ============================================================================
-- Indexes for the post queries of Phase 4 (pagination, sorting, filtering).
-- Run once on the real database:  mysql -u root -p bloggingPlatform < indexes.sql
-- (The first three already exist: 01_schema.sql of the previous project created them.)
-- ============================================================================

-- GET /api/posts?status=PUBLISHED  → WHERE status = ? ORDER BY created_at DESC LIMIT ?
CREATE INDEX idx_posts_status_created ON posts (status, created_at);

-- GET /api/posts?authorId=3        → WHERE author_id = ? ORDER BY created_at DESC LIMIT ?
CREATE INDEX idx_posts_author_created ON posts (author_id, created_at);

-- GET /api/posts?sortBy=title      → ORDER BY title LIMIT ?
CREATE INDEX idx_posts_title ON posts (title);

-- NEW in Phase 4. GET /api/posts (the default: no filter, newest first) → ORDER BY created_at DESC LIMIT ?
CREATE INDEX idx_posts_created_at ON posts (created_at);

-- Indexes that already exist without being written here:
--   * every PRIMARY KEY                                  (posts.post_id, users.user_id, ...)
--   * every UNIQUE constraint                            (users.username, users.email, tags.name,
--                                                         posts(author_id, title), reviews(post_id, user_id))
--   * every FOREIGN KEY column: InnoDB creates one itself (comments.post_id, reviews.post_id,
--                                                         post_tags.tag_id, ...). They make the
--                                                         comment count, the average rating and the
--                                                         tag filter fast.
--
-- Not created: a FULLTEXT index for the keyword search. LIKE '%word%' cannot use a normal index
-- (the text may start anywhere), so search reads every row. MATCH ... AGAINST with
--   CREATE FULLTEXT INDEX ft_posts_title_content ON posts (title, content);
-- is much faster on large tables (see the performance report), but it finds whole words only,
-- not parts of words, and the H2 test database does not support it.
