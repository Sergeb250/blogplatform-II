package com.amali.blogPlatform.algorithms;

import java.util.List;
import java.util.function.Function;

/**
 * Linear search: look at every item from the first to the last until one matches.
 * <ul>
 *   <li>Time: O(n). With 1,000 items it may compare all 1,000.</li>
 *   <li>Works on any list, sorted or not.</li>
 * </ul>
 */
public final class LinearSearch {

    private LinearSearch() {
    }

    /**
     * Finds the first item whose key equals {@code target} (ignoring upper/lower case).
     *
     * @param items  the list to search (any order)
     * @param keyOf  reads the key of an item, e.g. {@code PostResponse::title}
     * @param target the key to look for
     */
    public static <T> SearchResult search(List<T> items, Function<T, String> keyOf, String target) {
        int comparisons = 0;
        for (int i = 0; i < items.size(); i++) {
            comparisons++;
            if (keyOf.apply(items.get(i)).equalsIgnoreCase(target)) {
                return new SearchResult(i, comparisons);
            }
        }
        return new SearchResult(-1, comparisons);
    }
}
