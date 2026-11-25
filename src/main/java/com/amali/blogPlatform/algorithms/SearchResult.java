package com.amali.blogPlatform.algorithms;

/**
 * What a search found: the position of the item ({@code -1} when it is not there) and how many
 * items were compared on the way, which shows the difference between O(n) and O(log n).
 */
public record SearchResult(int index, int comparisons) {

    public boolean found() {
        return index >= 0;
    }
}
