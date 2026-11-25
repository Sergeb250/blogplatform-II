package com.amali.blogPlatform.model;

import com.amali.blogPlatform.exception.InvalidParameterException;

/**
 * The columns posts may be sorted by: the ALLOWED LIST.
 * <p>
 * A column name cannot be sent as a {@code ?} parameter (placeholders are only for values), so it
 * has to be written into the SQL text. Writing what the user typed into SQL would allow SQL
 * injection; instead the user's word is looked up here and only OUR fixed column text is used.
 */
public enum PostSortField {

    CREATED_AT("p.created_at"),
    TITLE("p.title"),
    RATING("average_rating");      // the alias of the AVG(...) column in PostRepository's SELECT

    private final String column;

    PostSortField(String column) {
        this.column = column;
    }

    /** The SQL text used in ORDER BY. */
    public String column() {
        return column;
    }

    /**
     * Turns the {@code sortBy} request parameter into a sort field.
     * Accepts {@code created_at}, {@code createdAt}, {@code date}, {@code title} and {@code rating}.
     */
    public static PostSortField fromParameter(String value) {
        if (value == null || value.isBlank()) {
            return CREATED_AT;
        }
        return switch (value.trim().toLowerCase()) {
            case "created_at", "createdat", "date" -> CREATED_AT;
            case "title" -> TITLE;
            case "rating" -> RATING;
            default -> throw new InvalidParameterException(
                    "sortBy must be one of: created_at, title, rating (was '" + value + "')");
        };
    }

    /** Turns the {@code direction} request parameter into true (ASC) or false (DESC). */
    public static boolean isAscending(String direction) {
        if (direction == null || direction.isBlank()) {
            return false;                                   // newest / highest first by default
        }
        return switch (direction.trim().toUpperCase()) {
            case "ASC" -> true;
            case "DESC" -> false;
            default -> throw new InvalidParameterException(
                    "direction must be ASC or DESC (was '" + direction + "')");
        };
    }
}
