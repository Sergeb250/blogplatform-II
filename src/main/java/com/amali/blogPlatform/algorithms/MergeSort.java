package com.amali.blogPlatform.algorithms;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Merge sort ("divide and conquer"): split the list in two halves, sort each half the same way,
 * then merge the two sorted halves by always taking the smaller front item.
 * <ul>
 *   <li>Time: always O(n log n), even in the worst case.</li>
 *   <li>Memory: O(n) extra (the halves are copied).</li>
 *   <li>Stable: items that compare as equal keep their original order.</li>
 * </ul>
 */
public final class MergeSort {

    private MergeSort() {
    }

    /** Returns a NEW sorted list; the input list is not changed. */
    public static <T> List<T> sort(List<T> items, Comparator<T> order) {
        if (items.size() <= 1) {
            return new ArrayList<>(items);              // 0 or 1 item is already sorted
        }
        int middle = items.size() / 2;
        List<T> left = sort(items.subList(0, middle), order);              // sort left half
        List<T> right = sort(items.subList(middle, items.size()), order);  // sort right half
        return merge(left, right, order);
    }

    /** Merges two sorted lists into one sorted list. */
    private static <T> List<T> merge(List<T> left, List<T> right, Comparator<T> order) {
        List<T> merged = new ArrayList<>(left.size() + right.size());
        int l = 0;
        int r = 0;
        while (l < left.size() && r < right.size()) {
            if (order.compare(left.get(l), right.get(r)) <= 0) {   // <= keeps equal items in order (stable)
                merged.add(left.get(l));
                l++;
            } else {
                merged.add(right.get(r));
                r++;
            }
        }
        while (l < left.size()) {          // copy what is left of the left half
            merged.add(left.get(l));
            l++;
        }
        while (r < right.size()) {         // copy what is left of the right half
            merged.add(right.get(r));
            r++;
        }
        return merged;
    }
}
