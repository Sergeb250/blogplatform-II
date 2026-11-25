package com.amali.blogPlatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** The totals shown on the admin dashboard. {@code averageRating} is null when there are no reviews. */
@Schema(description = "Site totals for the admin dashboard")
public record SiteSummaryResponse(long users, long admins, long authors, long readers, long posts,
                                  long published, long drafts, long comments, long reviews,
                                  Double averageRating) {
}
