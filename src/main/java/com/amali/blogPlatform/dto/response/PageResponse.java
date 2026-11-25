package com.amali.blogPlatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * One page of results plus what a client needs to show page buttons.
 *
 * @param content       the items on this page
 * @param page          this page's number, starting at 0
 * @param size          the maximum number of items per page
 * @param totalElements how many items match in total (all pages)
 * @param totalPages    how many pages there are
 * @param first         true on the first page
 * @param last          true on the last page
 */
@Schema(description = "One page of results with paging information")
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages,
                              boolean first, boolean last) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);   // e.g. 48 items, size 10 → 5 pages
        return new PageResponse<>(content, page, size, totalElements, totalPages,
                page == 0, page >= totalPages - 1);
    }
}
