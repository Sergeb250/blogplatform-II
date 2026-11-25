package com.amali.blogPlatform.controller;

import com.amali.blogPlatform.dto.response.PostResponse;
import com.amali.blogPlatform.dto.response.SiteSummaryResponse;
import com.amali.blogPlatform.dto.response.TagCountResponse;
import com.amali.blogPlatform.response.ApiResponse;
import com.amali.blogPlatform.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Analytics endpoints: site totals, posts per tag, top rated and most commented posts. */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @Operation(summary = "Site totals", description = "Users per role, posts per status, comments, reviews and the average rating.")
    @GetMapping("/summary")
    public ApiResponse<SiteSummaryResponse> summary() {
        return ApiResponse.ok("Summary retrieved successfully", reportService.getSummary());
    }

    @Operation(summary = "Posts per tag", description = "Every tag with its number of posts, most used first.")
    @GetMapping("/posts-per-tag")
    public ApiResponse<List<TagCountResponse>> postsPerTag() {
        return ApiResponse.ok("Posts per tag retrieved successfully", reportService.getPostsPerTag());
    }

    @Operation(summary = "Top rated posts", description = "Posts with at least one review, best average first.")
    @GetMapping("/top-rated")
    public ApiResponse<List<PostResponse>> topRated(@RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok("Top rated posts retrieved successfully", reportService.getTopRated(limit));
    }

    @Operation(summary = "Most commented posts", description = "Posts with at least one comment, most comments first.")
    @GetMapping("/most-commented")
    public ApiResponse<List<PostResponse>> mostCommented(@RequestParam(defaultValue = "5") int limit) {
        return ApiResponse.ok("Most commented posts retrieved successfully", reportService.getMostCommented(limit));
    }
}
