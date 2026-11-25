package com.amali.blogPlatform.algorithms;

import java.util.List;
import java.util.function.Function;

/**
 * Binary search: look at the middle item of a SORTED list; if the target is smaller, keep only the
 * left half, otherwise only the right half; repeat until found or nothing is left.
 * <ul>
 *   <li>Time: O(log n). With 1,000 items it needs at most about 10 comparisons.</li>
 *   <li>The list MUST be sorted by the same key (here: case-insensitive alphabetical order).</li>
 * </ul>
 */
public final class BinarySearch {

    private BinarySearch() {
    }

    /**
     * Finds an item whose key equals {@code target} (ignoring upper/lower case).
     *
     * @param sortedItems list sorted by {@code keyOf}, case-insensitive
     * @param keyOf       reads the key of an item, e.g. {@code PostResponse::title}
     * @param target      the key to look for
     */
    public static <T> SearchResult search(List<T> sortedItems, Function<T, String> keyOf, String target) {
        int low = 0;
        int high = sortedItems.size() - 1;
        int comparisons = 0;

        while (low <= high) {
            int middle = (low + high) / 2;
            String middleKey = keyOf.apply(sortedItems.get(middle));
            int result = middleKey.compareToIgnoreCase(target);
            comparisons++;

            if (result == 0) {
                return new SearchResult(middle, comparisons);   // found
            } else if (result < 0) {
                low = middle + 1;                               // target is in the right half
            } else {
                high = middle - 1;                              // target is in the left half
            }
        }
        return new SearchResult(-1, comparisons);               // not in the list
    }
}
