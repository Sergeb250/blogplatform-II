package com.amali.blogPlatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Typed application settings bound from the {@code blog.*} keys of the active profile's
 * properties file (for example {@code blog.environment=dev}). Spring creates this record once and
 * injects it wherever it is needed.
 *
 * @param name        display name of the API
 * @param version     API version
 * @param environment the environment the application runs in (dev, test or prod)
 * @param servers     one entry per profile: where that profile's API runs (shown in Swagger's "Servers" list)
 */
@ConfigurationProperties(prefix = "blog")
public record AppProperties(String name, String version, String environment, List<ApiServer> servers) {

    /** {@code servers} is optional in the properties; never hand out null. */
    public AppProperties {
        servers = servers == null ? List.of() : List.copyOf(servers);
    }

    /**
     * One environment the API can run in, bound from {@code blog.servers[0].profile=dev}, {@code .url=...}
     * and {@code .description=...}.
     */
    public record ApiServer(String profile, String url, String description) {
    }
}
