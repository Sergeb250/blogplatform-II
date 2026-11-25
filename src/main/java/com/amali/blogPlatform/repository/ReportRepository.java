package com.amali.blogPlatform.repository;

import com.amali.blogPlatform.dto.response.SiteSummaryResponse;
import com.amali.blogPlatform.dto.response.TagCountResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/** SQL for the analytics reports: counts and averages computed by the database (GROUP BY, COUNT, AVG). */
@Repository
public class ReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Every total of the dashboard in ONE query, each from a small subquery. Deleted rows are not counted. */
    public SiteSummaryResponse getSummary() {
        String sql = """
                SELECT
                  (SELECT COUNT(*) FROM users WHERE deleted_at IS NULL)                         AS users,
                  (SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND role = 'ADMIN')      AS admins,
                  (SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND role = 'AUTHOR')     AS authors,
                  (SELECT COUNT(*) FROM users WHERE deleted_at IS NULL AND role = 'READER')     AS readers,
                  (SELECT COUNT(*) FROM posts WHERE deleted_at IS NULL)                         AS posts,
                  (SELECT COUNT(*) FROM posts WHERE deleted_at IS NULL AND status = 'PUBLISHED') AS published,
                  (SELECT COUNT(*) FROM posts WHERE deleted_at IS NULL AND status = 'DRAFT')    AS drafts,
                  (SELECT COUNT(*) FROM comments WHERE deleted_at IS NULL)                      AS comments,
                  (SELECT COUNT(*) FROM reviews WHERE deleted_at IS NULL)                       AS reviews,
                  (SELECT AVG(rating) FROM reviews WHERE deleted_at IS NULL)                    AS average_rating
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            BigDecimal average = rs.getBigDecimal("average_rating");       // null when there are no reviews
            return new SiteSummaryResponse(rs.getLong("users"), rs.getLong("admins"), rs.getLong("authors"),
                    rs.getLong("readers"), rs.getLong("posts"), rs.getLong("published"), rs.getLong("drafts"),
                    rs.getLong("comments"), rs.getLong("reviews"), average == null ? null : average.doubleValue());
        });
    }

    /**
     * Every active tag with the number of active posts that have it, most used first (tags without
     * posts count 0). COUNT(p.post_id) skips the NULLs of the LEFT JOIN, so deleted posts are not counted.
     */
    public List<TagCountResponse> getPostsPerTag() {
        String sql = """
                SELECT t.name, COUNT(p.post_id) AS posts
                FROM tags t
                LEFT JOIN post_tags pt ON pt.tag_id = t.tag_id
                LEFT JOIN posts p ON p.post_id = pt.post_id AND p.deleted_at IS NULL
                WHERE t.deleted_at IS NULL
                GROUP BY t.tag_id, t.name
                ORDER BY posts DESC, t.name
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TagCountResponse(rs.getString("name"), rs.getLong("posts")));
    }
}
