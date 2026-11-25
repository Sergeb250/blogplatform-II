package com.amali.blogPlatform.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Timing statistics of one service method, measured by the AOP aspect. */
@Schema(description = "Timing statistics of one service method, measured by AOP")
public record MethodMetricsResponse(String method, long calls, long errors, double totalMillis,
                                    double averageMillis, double maxMillis) {
}
