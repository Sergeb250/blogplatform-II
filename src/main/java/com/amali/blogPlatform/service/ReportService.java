package com.amali.blogPlatform.service;

import com.amali.blogPlatform.dto.response.PostResponse;
import com.amali.blogPlatform.dto.response.SiteSummaryResponse;
import com.amali.blogPlatform.dto.response.TagCountResponse;
import com.amali.blogPlatform.exception.InvalidParameterException;
import com.amali.blogPlatform.repository.PostRepository;
import com.amali.blogPlatform.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Analytics for the admin dashboard. The counting and sorting is done by the database. */
@Service
@Transactional(readOnly = true)
public class ReportService {

    private static final int MAX_LIMIT = 50;

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;

    public ReportService(ReportRepository reportRepository, PostRepository postRepository) {
        this.reportRepository = reportRepository;
        this.postRepository = postRepository;
    }

    public SiteSummaryResponse getSummary() {
        return reportRepository.getSummary();
    }

    public List<TagCountResponse> getPostsPerTag() {
        return reportRepository.getPostsPerTag();
    }

    public List<PostResponse> getTopRated(int limit) {
        checkLimit(limit);
        return postRepository.findTopRated(limit).stream().map(PostResponse::from).toList();
    }

    public List<PostResponse> getMostCommented(int limit) {
        checkLimit(limit);
        return postRepository.findMostCommented(limit).stream().map(PostResponse::from).toList();
    }

    private static void checkLimit(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new InvalidParameterException("limit must be between 1 and " + MAX_LIMIT + " (was " + limit + ")");
        }
    }
}
