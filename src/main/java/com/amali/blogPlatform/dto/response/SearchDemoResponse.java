package com.amali.blogPlatform.dto.response;

/**
 * Result of the searching demo: whether the title was found, after how many comparisons, among how
 * many posts, and how long the search took. {@code post} is null when nothing was found.
 */
public record SearchDemoResponse(String algorithm, String title, boolean found, int comparisons,
                                 int itemCount, double durationMillis, PostResponse post) {
}
