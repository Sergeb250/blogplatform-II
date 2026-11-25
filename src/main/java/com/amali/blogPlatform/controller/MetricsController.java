package com.amali.blogPlatform.controller;

import com.amali.blogPlatform.aspect.PerformanceMonitor;
import com.amali.blogPlatform.dto.response.MethodMetricsResponse;
import com.amali.blogPlatform.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Shows the timings that the AOP {@code LoggingAspect} measured for every service method. */
@RestController
@RequestMapping("/api/metrics")
@Tag(name = "System")
public class MetricsController {

    private final PerformanceMonitor monitor;

    public MetricsController(PerformanceMonitor monitor) {
        this.monitor = monitor;
    }

    @Operation(summary = "Service method timings (AOP)",
            description = "Calls, errors, total, average and maximum time of every service method since start or the last reset.")
    @GetMapping("/methods")
    public ApiResponse<List<MethodMetricsResponse>> methods() {
        return ApiResponse.ok("Method metrics retrieved successfully", monitor.snapshot());
    }

    @Operation(summary = "Reset the timings")
    @DeleteMapping("/methods")
    public ResponseEntity<Void> reset() {
        monitor.reset();
        return ResponseEntity.noContent().build();
    }
}
