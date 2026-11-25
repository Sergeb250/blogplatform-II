package com.amali.blogPlatform.repository;

import com.amali.blogPlatform.model.Post;
import com.amali.blogPlatform.model.PostFilter;
import com.amali.blogPlatform.model.PostSortField;
import com.amali.blogPlatform.model.PostStatus;
import com.amali.blogPlatform.model.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SQL for {@code posts} and the {@code post_tags} junction table.
 * <p>
 * Reading a list of posts takes two queries, never one per post: the posts (with author name,
 * comment count and average rating), then the tags of all of them with one {@code IN (...)} query.
 */
@Repository
public class PostRepository {

    /**
     * Every column of an ACTIVE post (soft delete: {@code deleted_at IS NULL}), plus the author's username,
     * the number of comments and the average rating (also counting only active comments and reviews).
     * It already ends with a WHERE, so callers add more conditions with " AND ...".
     */
    private static final String SELECT_POSTS = """
            SELECT p.post_id, p.author_id, u.username AS author_username, p.title, p.content, p.status,
                   p.created_at, p.updated_at,
                   (SELECT COUNT(*) FROM comments c WHERE c.post_id = p.post_id AND c.deleted_at IS NULL) AS comment_count,
                   (SELECT AVG(r.rating) FROM reviews r WHERE r.post_id = p.post_id AND r.deleted_at IS NULL) AS average_rating
            FROM posts p
            JOIN users u ON u.user_id = p.author_id
            WHERE p.deleted_at IS NULL
            """;

    private static final RowMapper<Post> POST_ROW_MAPPER = (rs, rowNum) -> {
        BigDecimal average = rs.getBigDecimal("average_rating");   // null when the post has no reviews
        return new Post(
                rs.getLong("post_id"),
                rs.getLong("author_id"),
                rs.getString("author_username"),
                rs.getString("title"),
                rs.getString("content"),
                PostStatus.valueOf(rs.getString("status")),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class),
                rs.getInt("comment_count"),
                average == null ? null : average.doubleValue(),
                List.of());
    };

    private static final RowMapper<Tag> TAG_ROW_MAPPER =
            (rs, rowNum) -> new Tag(rs.getLong("tag_id"), rs.getString("name"));

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    public PostRepository(JdbcTemplate jdbcTemplate, NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    /** All posts, newest first, with their tags. */
    public List<Post> findAll() {
        List<Post> posts = jdbcTemplate.query(
                SELECT_POSTS + " ORDER BY p.created_at DESC, p.post_id DESC", POST_ROW_MAPPER);   // SELECT_POSTS already skips deleted posts
        return withTags(posts);
    }

    /**
     * One page of posts that match the filter, in the chosen order, with their tags.
     * Example of the SQL for {@code ?authorId=3&sortBy=title&direction=ASC&page=1&size=10}:
     * <pre>
     * SELECT ... WHERE p.deleted_at IS NULL AND p.author_id = ? ORDER BY p.title ASC, p.post_id ASC LIMIT ? OFFSET ?
     * values: [3, 10, 10]
     * </pre>
     */
    public List<Post> findPage(PostFilter filter, PostSortField sortField, boolean ascending,
                               int limit, int offset) {
        List<Object> values = new ArrayList<>();
        String direction = ascending ? "ASC" : "DESC";
        String sql = SELECT_POSTS
                + filterConditions(filter, values)
                + " ORDER BY " + sortField.column() + " " + direction   // column text comes from the enum, never from the user
                + ", p.post_id " + direction                           // tie-breaker: a stable order between pages
                + " LIMIT ? OFFSET ?";
        values.add(limit);
        values.add(offset);
        List<Post> posts = jdbcTemplate.query(sql, POST_ROW_MAPPER, values.toArray());
        return withTags(posts);
    }

    /** The best-rated posts (only posts with at least one review); ties: more comments first. */
    public List<Post> findTopRated(int limit) {
        return withTags(jdbcTemplate.query("SELECT * FROM (" + SELECT_POSTS + ") ranked"
                        + " WHERE average_rating IS NOT NULL"
                        + " ORDER BY average_rating DESC, comment_count DESC, post_id DESC LIMIT ?",
                POST_ROW_MAPPER, limit));
    }

    /** The posts with the most comments (only posts that have comments). */
    public List<Post> findMostCommented(int limit) {
        return withTags(jdbcTemplate.query("SELECT * FROM (" + SELECT_POSTS + ") ranked"
                        + " WHERE comment_count > 0"
                        + " ORDER BY comment_count DESC, post_id DESC LIMIT ?",
                POST_ROW_MAPPER, limit));
    }

    /** How many posts match the filter (for totalElements / totalPages). */
    public long countPosts(PostFilter filter) {
        List<Object> values = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM posts p WHERE p.deleted_at IS NULL" + filterConditions(filter, values);
        Long count = jdbcTemplate.queryForObject(sql, Long.class, values.toArray());
        return count == null ? 0 : count;
    }

    /**
     * Builds one " AND ..." for every filter that is set, each with a {@code ?} placeholder whose value
     * is added to {@code values}. The query already has "WHERE p.deleted_at IS NULL", so every
     * condition can simply start with "AND".
     */
    private String filterConditions(PostFilter filter, List<Object> values) {
        StringBuilder sql = new StringBuilder();
        if (filter.authorId() != null) {
            sql.append(" AND p.author_id = ?");
            values.add(filter.authorId());
        }
        if (filter.status() != null) {
            sql.append(" AND p.status = ?");
            values.add(filter.status().name());
        }
        if (filter.tag() != null && !filter.tag().isBlank()) {
            sql.append(" AND p.post_id IN (SELECT pt.post_id FROM post_tags pt"
                    + " JOIN tags t ON t.tag_id = pt.tag_id WHERE t.name = ? AND t.deleted_at IS NULL)");
            values.add(filter.tag().trim());
        }
        if (filter.keyword() != null && !filter.keyword().isBlank()) {
            // MySQL's default collation ignores upper/lower case, so "java" also finds "Java"
            sql.append(" AND (p.title LIKE ? OR p.content LIKE ?)");
            String pattern = "%" + escapeLike(filter.keyword().trim()) + "%";
            values.add(pattern);
            values.add(pattern);
        }
        return sql.toString();
    }

    /** % and _ are wildcards in LIKE; escape them so a search for "50%" finds the text "50%". */
    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    public Optional<Post> findById(long id) {
        List<Post> posts = jdbcTemplate.query(SELECT_POSTS + " AND p.post_id = ?", POST_ROW_MAPPER, id);
        return withTags(posts).stream().findFirst();
    }

    /** True only for an ACTIVE post; a soft-deleted post counts as "not found". */
    public boolean existsById(long id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM posts WHERE post_id = ? AND deleted_at IS NULL", Long.class, id);
        return count != null && count > 0;
    }

    /**
     * True if ANOTHER post already has this title, whoever wrote it: two posts may never share a title.
     * Upper/lower case and spaces around the title are ignored ("Java Tips" = " java tips ").
     * {@code excludePostId} is the post being edited (it may keep its own title), or 0 for a new post.
     * Deleted posts are included on purpose: like usernames, the title of a deleted post stays reserved,
     * and the table's UNIQUE (author_id, title) would reject it for the same author anyway.
     */
    public boolean existsByTitle(String title, long excludePostId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM posts WHERE LOWER(TRIM(title)) = LOWER(TRIM(?)) AND post_id <> ?",
                Long.class, title, excludePostId);
        return count != null && count > 0;
    }

    /** Inserts the post (without tags) and returns its generated {@code post_id}. */
    public long insert(Post post) {
        String sql = "INSERT INTO posts (author_id, title, content, status) VALUES (?, ?, ?, ?)";
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"post_id"});
            ps.setLong(1, post.authorId());
            ps.setString(2, post.title());
            ps.setString(3, post.content());
            ps.setString(4, post.status().name());
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    /** Updates the post's own columns (not its tags); returns the number of rows changed. */
    public int update(Post post) {
        return jdbcTemplate.update("""
                UPDATE posts SET author_id = ?, title = ?, content = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE post_id = ? AND deleted_at IS NULL
                """, post.authorId(), post.title(), post.content(), post.status().name(), post.id());
    }

    /**
     * Soft delete: marks the post as deleted instead of removing the row. Returns 0 when the post does
     * not exist or is already deleted. Its comments and reviews are soft-deleted by the service.
     */
    public int softDeleteById(long id) {
        return jdbcTemplate.update(
                "UPDATE posts SET deleted_at = CURRENT_TIMESTAMP WHERE post_id = ? AND deleted_at IS NULL", id);
    }

    /** Soft-deletes every active post of one author (used when the author is deleted). */
    public int softDeleteByAuthorId(long authorId) {
        return jdbcTemplate.update(
                "UPDATE posts SET deleted_at = CURRENT_TIMESTAMP WHERE author_id = ? AND deleted_at IS NULL", authorId);
    }

    /**
     * Replaces the post's tags: deletes the old links, then inserts the new ones in one JDBC batch.
     * Call it inside a {@code @Transactional} service method so both statements commit together.
     */
    public void replaceTags(long postId, Collection<Long> tagIds) {
        jdbcTemplate.update("DELETE FROM post_tags WHERE post_id = ?", postId);
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<Object[]> rows = new ArrayList<>();
        for (Long tagId : tagIds) {
            rows.add(new Object[]{postId, tagId});
        }
        jdbcTemplate.batchUpdate("INSERT INTO post_tags (post_id, tag_id) VALUES (?, ?)", rows);
    }

    /** The (active) tags of one post, by name. */
    public List<Tag> findTagsByPostId(long postId) {
        return jdbcTemplate.query("""
                SELECT t.tag_id, t.name FROM post_tags pt JOIN tags t ON t.tag_id = pt.tag_id
                WHERE pt.post_id = ? AND t.deleted_at IS NULL ORDER BY t.name
                """, TAG_ROW_MAPPER, postId);
    }

    /** Adds the (active) tags to every post with a single {@code IN (:ids)} query. */
    private List<Post> withTags(List<Post> posts) {
        if (posts.isEmpty()) {
            return posts;
        }
        List<Long> ids = posts.stream().map(Post::id).toList();
        Map<Long, List<Tag>> tagsByPost = new HashMap<>();
        namedJdbcTemplate.query("""
                SELECT pt.post_id, t.tag_id, t.name FROM post_tags pt JOIN tags t ON t.tag_id = pt.tag_id
                WHERE pt.post_id IN (:ids) AND t.deleted_at IS NULL ORDER BY t.name
                """, new MapSqlParameterSource("ids", ids), rs -> {
            tagsByPost.computeIfAbsent(rs.getLong("post_id"), id -> new ArrayList<>())
                    .add(new Tag(rs.getLong("tag_id"), rs.getString("name")));
        });
        return posts.stream()
                .map(post -> post.withTags(tagsByPost.getOrDefault(post.id(), List.of())))
                .toList();
    }
}
