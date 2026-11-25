package com.amali.blogPlatform.service;

import com.amali.blogPlatform.algorithms.BinarySearch;
import com.amali.blogPlatform.algorithms.LinearSearch;
import com.amali.blogPlatform.algorithms.MergeSort;
import com.amali.blogPlatform.algorithms.SearchResult;
import com.amali.blogPlatform.dto.response.PostResponse;
import com.amali.blogPlatform.dto.response.SearchDemoResponse;
import com.amali.blogPlatform.dto.response.SortDemoResponse;
import com.amali.blogPlatform.exception.InvalidParameterException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Demonstrates the searching and sorting algorithms on the real posts, with timings.
 * <p>
 * The API itself sorts and searches with SQL (ORDER BY, LIKE, indexes), which scales to millions of
 * rows. This class shows the same ideas in Java, as the brief asks: "demonstrate algorithmic
 * knowledge separately and use database-native techniques for scalable API operations".
 */
@Service
public class AlgorithmService {

    private final PostService postService;

    public AlgorithmService(PostService postService) {
        this.postService = postService;
    }

    /** Sorts every post with merge sort and compares the time with Java's built-in List.sort. */
    public SortDemoResponse sortPosts(String sortBy) {
        List<PostResponse> posts = postService.getAllPosts();
        Comparator<PostResponse> order = comparatorFor(sortBy);

        long start = System.nanoTime();
        List<PostResponse> sorted = MergeSort.sort(posts, order);
        double durationMillis = millisSince(start);

        List<PostResponse> copy = new ArrayList<>(posts);
        long javaStart = System.nanoTime();
        copy.sort(order);                                   // Java's built-in sort (TimSort), for comparison
        double javaMillis = millisSince(javaStart);

        List<String> firstTitles = sorted.stream().limit(10).map(PostResponse::title).toList();
        return new SortDemoResponse("merge", sortBy, posts.size(), durationMillis, javaMillis, firstTitles);
    }

    /**
     * Finds a post by its exact title (ignoring case) with linear search or binary search.
     * Binary search needs a sorted list, so the posts are first sorted by title with merge sort;
     * that sorting is not part of the measured search time.
     */
    public SearchDemoResponse searchByTitle(String algorithm, String title) {
        if (title == null || title.isBlank()) {
            throw new InvalidParameterException("title must not be empty");
        }
        List<PostResponse> posts = postService.getAllPosts();
        Comparator<PostResponse> byTitle = Comparator.comparing(PostResponse::title, String.CASE_INSENSITIVE_ORDER);

        List<PostResponse> list;
        SearchResult result;
        long start;
        switch (algorithm.toLowerCase()) {
            case "linear" -> {
                list = posts;                                   // any order is fine
                start = System.nanoTime();
                result = LinearSearch.search(list, PostResponse::title, title);
            }
            case "binary" -> {
                list = MergeSort.sort(posts, byTitle);          // must be sorted first
                start = System.nanoTime();
                result = BinarySearch.search(list, PostResponse::title, title);
            }
            default -> throw new InvalidParameterException("algorithm must be linear or binary (was '" + algorithm + "')");
        }
        double durationMillis = millisSince(start);

        PostResponse found = result.found() ? list.get(result.index()) : null;
        return new SearchDemoResponse(algorithm.toLowerCase(), title, result.found(), result.comparisons(),
                list.size(), durationMillis, found);
    }

    private static Comparator<PostResponse> comparatorFor(String sortBy) {
        return switch (sortBy == null ? "title" : sortBy.toLowerCase()) {
            case "title" -> Comparator.comparing(PostResponse::title, String.CASE_INSENSITIVE_ORDER);
            case "created_at", "date" -> Comparator.comparing(PostResponse::createdAt);
            case "rating" -> Comparator.comparing(PostResponse::averageRating,
                    Comparator.nullsFirst(Comparator.naturalOrder()));   // posts without reviews first
            default -> throw new InvalidParameterException("sortBy must be title, created_at or rating (was '" + sortBy + "')");
        };
    }

    private static double millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000.0;
    }
}
