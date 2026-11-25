package com.amali.blogPlatform.service;

import com.amali.blogPlatform.config.AppProperties;
import com.amali.blogPlatform.dto.response.SystemInfoResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Reports which application, version and profile are running. It shows constructor injection:
 * Spring passes the {@link AppProperties} bean and the {@link Environment} to the constructor, and
 * the fields stay {@code final}.
 */
@Service
public class SystemInfoService {

    private final AppProperties properties;
    private final Environment environment;

    public SystemInfoService(AppProperties properties, Environment environment) {
        this.properties = properties;
        this.environment = environment;
    }

    public SystemInfoResponse getInfo() {
        return new SystemInfoResponse(
                properties.name(),
                properties.version(),
                properties.environment(),
                activeProfiles(),
                Runtime.version().toString());
    }

    /** The profiles set explicitly, or the default profile ("dev") when none was set. */
    private List<String> activeProfiles() {
        String[] active = environment.getActiveProfiles();
        return List.of(active.length > 0 ? active : environment.getDefaultProfiles());
    }
}
