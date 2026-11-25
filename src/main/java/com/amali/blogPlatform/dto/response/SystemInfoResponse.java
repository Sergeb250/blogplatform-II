package com.amali.blogPlatform.dto.response;

import java.util.List;

/** What {@code GET /api/system/info} returns: the running application and its active profiles. */
public record SystemInfoResponse(String name, String version, String environment,
                                 List<String> activeProfiles, String javaVersion) {
}
