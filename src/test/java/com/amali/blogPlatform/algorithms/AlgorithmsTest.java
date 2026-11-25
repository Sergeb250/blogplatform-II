package com.amali.blogPlatform.algorithms;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class AlgorithmsTest {

    private static final Comparator<String> ALPHABETICAL = String.CASE_INSENSITIVE_ORDER;

    private static List<String> randomWords(int count) {
        Random random = new Random(42);                    // fixed seed: the same "random" list every run
        List<String> words = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            words.add("word" + random.nextInt(10_000));
        }
        return words;
    }

    @Test
    void mergeSortSortsAndKeepsInputUnchanged() {
        List<String> input = List.of("pear", "Apple", "banana", "cherry");
        assertThat(MergeSort.sort(input, ALPHABETICAL)).containsExactly("Apple", "banana", "cherry", "pear");
        assertThat(input).containsExactly("pear", "Apple", "banana", "cherry");
    }

    @Test
    void mergeSortAgreesWithJavaOnALargeList() {
        List<String> words = randomWords(5_000);
        List<String> expected = new ArrayList<>(words);
        expected.sort(ALPHABETICAL);

        assertThat(MergeSort.sort(words, ALPHABETICAL)).isEqualTo(expected);
    }

    @Test
    void mergeSortHandlesEmptyOneItemAndAlreadySortedLists() {
        assertThat(MergeSort.sort(List.<String>of(), ALPHABETICAL)).isEmpty();
        assertThat(MergeSort.sort(List.of("only"), ALPHABETICAL)).containsExactly("only");
        assertThat(MergeSort.sort(List.of("a", "b", "c", "d"), ALPHABETICAL)).containsExactly("a", "b", "c", "d");
    }

    @Test
    void mergeSortIsStable() {
        record Item(String key, int originalPosition) { }
        List<Item> items = List.of(new Item("b", 1), new Item("a", 2), new Item("b", 3), new Item("a", 4));

        List<Item> sorted = MergeSort.sort(items, Comparator.comparing(Item::key));

        assertThat(sorted).extracting(Item::originalPosition).containsExactly(2, 4, 1, 3);   // equal keys keep their order
    }

    @Test
    void linearSearchChecksItemsOneByOne() {
        List<String> words = List.of("delta", "alpha", "charlie", "bravo");

        assertThat(LinearSearch.search(words, Function.identity(), "CHARLIE"))
                .isEqualTo(new SearchResult(2, 3));
        assertThat(LinearSearch.search(words, Function.identity(), "zulu"))
                .isEqualTo(new SearchResult(-1, 4));                  // compared every item
    }

    @Test
    void binarySearchNeedsFarFewerComparisons() {
        List<String> sorted = new ArrayList<>();
        for (int i = 0; i < 1_000; i++) {
            sorted.add(String.format("title%04d", i));              // title0000 ... title0999, already sorted
        }

        SearchResult binary = BinarySearch.search(sorted, Function.identity(), "title0777");
        SearchResult linear = LinearSearch.search(sorted, Function.identity(), "title0777");

        assertThat(binary.index()).isEqualTo(777);
        assertThat(linear.index()).isEqualTo(777);
        assertThat(binary.comparisons()).isLessThanOrEqualTo(10);   // log2(1000) ≈ 10
        assertThat(linear.comparisons()).isEqualTo(778);
        assertThat(BinarySearch.search(sorted, Function.identity(), "missing").found()).isFalse();
    }
}
