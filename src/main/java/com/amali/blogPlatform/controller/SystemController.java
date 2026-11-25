package com.amali.blogPlatform.controller;

import com.amali.blogPlatform.dto.response.SystemInfoResponse;
import com.amali.blogPlatform.response.ApiResponse;
import com.amali.blogPlatform.service.SystemInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Shows that the application is up and which profile is active. */
@RestController
@Tag(name = "System")
@RequestMapping("/api/system")
public class SystemController {

    private final SystemInfoService systemInfoService;

    public SystemController(SystemInfoService systemInfoService) {
        this.systemInfoService = systemInfoService;
    }

    @Operation(summary = "Application status", description = "Name, version, environment and active Spring profiles.")
    @GetMapping("/info")
    public ApiResponse<SystemInfoResponse> info() {
        return ApiResponse.ok("Application is running", systemInfoService.getInfo());
    }
}
