package com.amali.blogPlatform.dto.response;

import java.util.List;

/**
 * Result of the sorting demo: which algorithm sorted how many posts, how long it took, how long
 * Java's own {@code List.sort} took on the same data, and the first titles in the sorted order.
 */
public record SortDemoResponse(String algorithm, String sortBy, int itemCount,
                               double durationMillis, double javaListSortMillis,
                               List<String> firstTitles) {
}
