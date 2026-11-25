package com.amali.blogPlatform.repository;

import com.amali.blogPlatform.model.Tag;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

/** SQL for the {@code tags} table. Soft delete: reads return only ACTIVE tags ({@code deleted_at IS NULL}). */
@Repository
public class TagRepository {

    private static final RowMapper<Tag> TAG_ROW_MAPPER =
            (rs, rowNum) -> new Tag(rs.getLong("tag_id"), rs.getString("name"));

    private final JdbcTemplate jdbcTemplate;

    public TagRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Tag> findAll() {
        return jdbcTemplate.query("SELECT tag_id, name FROM tags WHERE deleted_at IS NULL ORDER BY name", TAG_ROW_MAPPER);
    }

    public Optional<Tag> findById(long id) {
        return jdbcTemplate.query("SELECT tag_id, name FROM tags WHERE tag_id = ? AND deleted_at IS NULL",
                        TAG_ROW_MAPPER, id)
                .stream().findFirst();
    }

    /** Active tag with this name. */
    public Optional<Tag> findByName(String name) {
        return jdbcTemplate.query("SELECT tag_id, name FROM tags WHERE name = ? AND deleted_at IS NULL",
                        TAG_ROW_MAPPER, name)
                .stream().findFirst();
    }

    /** True only for an ACTIVE tag; a soft-deleted tag counts as "not found". */
    public boolean existsById(long id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tags WHERE tag_id = ? AND deleted_at IS NULL", Long.class, id);
        return count != null && count > 0;
    }

    /**
     * True if a tag other than {@code tagIdToIgnore} has this name, ALSO a deleted one: its row still
     * exists, so the UNIQUE constraint would reject the name.
     */
    public boolean isNameTakenByAnother(String name, long tagIdToIgnore) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tags WHERE name = ? AND tag_id <> ?", Long.class, name, tagIdToIgnore);
        return count != null && count > 0;
    }

    /**
     * Brings back a soft-deleted tag with this name (creating a tag that was deleted before restores it,
     * because the name is UNIQUE). Returns its id, or empty when no deleted tag has this name.
     */
    public Optional<Long> restoreDeletedByName(String name) {
        int restored = jdbcTemplate.update(
                "UPDATE tags SET deleted_at = NULL WHERE name = ? AND deleted_at IS NOT NULL", name);
        return restored == 0 ? Optional.empty() : findByName(name).map(Tag::id);
    }

    /** Inserts the tag and returns its generated {@code tag_id}. */
    public long insert(Tag tag) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO tags (name) VALUES (?)", new String[]{"tag_id"});
            ps.setString(1, tag.name());
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    public int update(Tag tag) {
        return jdbcTemplate.update("UPDATE tags SET name = ? WHERE tag_id = ? AND deleted_at IS NULL",
                tag.name(), tag.id());
    }

    /**
     * Soft delete: marks the tag as deleted. Its links in {@code post_tags} stay, but every query that
     * reads tags skips deleted ones, so the tag disappears from all posts. Returns 0 when the tag does
     * not exist or is already deleted.
     */
    public int softDeleteById(long id) {
        return jdbcTemplate.update(
                "UPDATE tags SET deleted_at = CURRENT_TIMESTAMP WHERE tag_id = ? AND deleted_at IS NULL", id);
    }
}
