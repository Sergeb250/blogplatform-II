package com.amali.blogPlatform.controller;

import com.amali.blogPlatform.dto.response.SearchDemoResponse;
import com.amali.blogPlatform.dto.response.SortDemoResponse;
import com.amali.blogPlatform.response.ApiResponse;
import com.amali.blogPlatform.service.AlgorithmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Demo endpoints that run the Java searching and sorting algorithms on the real posts and time them. */
@RestController
@Tag(name = "Algorithms")
@RequestMapping("/api/algorithms")
public class AlgorithmController {

    private final AlgorithmService algorithmService;

    public AlgorithmController(AlgorithmService algorithmService) {
        this.algorithmService = algorithmService;
    }

    /** e.g. {@code GET /api/algorithms/sort?sortBy=rating} */
    @Operation(summary = "Sort all posts with merge sort", description = "sortBy = title, created_at or rating. Returns the time taken and the time of Java's List.sort on the same data.")
    @GetMapping("/sort")
    public ApiResponse<SortDemoResponse> sort(@RequestParam(defaultValue = "title") String sortBy) {
        return ApiResponse.ok("Posts sorted with merge sort", algorithmService.sortPosts(sortBy));
    }

    /** e.g. {@code GET /api/algorithms/search?algorithm=binary&title=JAVA} */
    @Operation(summary = "Find a post by exact title with linear or binary search", description = "Returns whether it was found and how many comparisons were needed: O(n) for linear, O(log n) for binary.")
    @GetMapping("/search")
    public ApiResponse<SearchDemoResponse> search(@RequestParam(defaultValue = "binary") String algorithm,
                                                  @RequestParam String title) {
        return ApiResponse.ok("Search finished with " + algorithm + " search",
                algorithmService.searchByTitle(algorithm, title));
    }
}
