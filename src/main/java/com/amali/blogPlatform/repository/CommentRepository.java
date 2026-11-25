package com.amali.blogPlatform.repository;

import com.amali.blogPlatform.model.Comment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** SQL for the {@code comments} table. Soft delete: reads return only ACTIVE comments. */
@Repository
public class CommentRepository {

    /** Active comments only. It already ends with a WHERE, so callers add conditions with " AND ...". */
    private static final String SELECT_COMMENTS = """
            SELECT c.comment_id, c.post_id, p.title AS post_title, c.user_id, u.username, c.content, c.created_at
            FROM comments c
            JOIN users u ON u.user_id = c.user_id
            JOIN posts p ON p.post_id = c.post_id
            WHERE c.deleted_at IS NULL
            """;

    private static final RowMapper<Comment> COMMENT_ROW_MAPPER = (rs, rowNum) -> new Comment(
            rs.getLong("comment_id"),
            rs.getLong("post_id"),
            rs.getString("post_title"),
            rs.getLong("user_id"),
            rs.getString("username"),
            rs.getString("content"),
            rs.getObject("created_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public CommentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** The comments of a post, oldest first (reading order). */
    public List<Comment> findByPostId(long postId) {
        return jdbcTemplate.query(SELECT_COMMENTS + " AND c.post_id = ? ORDER BY c.created_at, c.comment_id",
                COMMENT_ROW_MAPPER, postId);
    }

    /**
     * Every row, or only those written by {@code userId}, or only those on posts written by
     * {@code postAuthorId} (null = no filter). Newest first.
     */
    public List<Comment> findAll(Long userId, Long postAuthorId) {
        List<Object> values = new ArrayList<>();
        StringBuilder sql = new StringBuilder(SELECT_COMMENTS);
        if (userId != null) {
            sql.append(" AND c.user_id = ?");
            values.add(userId);
        }
        if (postAuthorId != null) {
            sql.append(" AND p.author_id = ?");
            values.add(postAuthorId);
        }
        sql.append(" ORDER BY c.created_at DESC, c.comment_id DESC");
        return jdbcTemplate.query(sql.toString(), COMMENT_ROW_MAPPER, values.toArray());
    }

    public Optional<Comment> findById(long id) {
        return jdbcTemplate.query(SELECT_COMMENTS + " AND c.comment_id = ?", COMMENT_ROW_MAPPER, id)
                .stream().findFirst();
    }

    /** Inserts the comment and returns its generated {@code comment_id}. */
    public long insert(Comment comment) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO comments (post_id, user_id, content) VALUES (?, ?, ?)",
                    new String[]{"comment_id"});
            ps.setLong(1, comment.postId());
            ps.setLong(2, comment.userId());
            ps.setString(3, comment.content());
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /** Only the text of a comment can change. */
    public int updateContent(long id, String content) {
        return jdbcTemplate.update(
                "UPDATE comments SET content = ? WHERE comment_id = ? AND deleted_at IS NULL", content, id);
    }

    /** Soft delete: marks the comment as deleted. Returns 0 when it does not exist or is already deleted. */
    public int softDeleteById(long id) {
        return jdbcTemplate.update(
                "UPDATE comments SET deleted_at = CURRENT_TIMESTAMP WHERE comment_id = ? AND deleted_at IS NULL", id);
    }

    /** Soft-deletes every active comment of a post (used when the post is deleted). */
    public int softDeleteByPostId(long postId) {
        return jdbcTemplate.update(
                "UPDATE comments SET deleted_at = CURRENT_TIMESTAMP WHERE post_id = ? AND deleted_at IS NULL", postId);
    }

    /**
     * Soft-deletes every active comment written by the user OR on a post written by the user
     * (used when the user is deleted).
     */
    public int softDeleteByUserId(long userId) {
        return jdbcTemplate.update("""
                UPDATE comments SET deleted_at = CURRENT_TIMESTAMP
                WHERE deleted_at IS NULL
                  AND (user_id = ? OR post_id IN (SELECT post_id FROM posts WHERE author_id = ?))
                """, userId, userId);
    }
}
