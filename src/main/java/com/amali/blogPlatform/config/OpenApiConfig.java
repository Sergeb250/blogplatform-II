package com.amali.blogPlatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;
import java.util.List;

/**
 * The general information shown at the top of Swagger UI ({@code /swagger-ui/index.html}).
 * Springdoc reads every controller and DTO by itself and builds the OpenAPI document at
 * {@code /v3/api-docs}; this bean only adds the title, description, version and the tag groups.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bloggingApi(AppProperties properties) {
        return new OpenAPI()
                .info(new Info()
                        .title(properties.name())
                        .version(properties.version())
                        .description("""
                                REST API of the Blogging Platform: users, posts, comments, tags and reviews.
                                Data is stored in MySQL and read with Spring JDBC (JdbcTemplate, no JPA).

                                Every response has the shape `{ "status": 200, "message": "...", "data": ... }`.
                                Errors use the same shape; for validation errors `data` holds one message per field.

                                The same data is also available through GraphQL at `/graphql` (GraphiQL: `/graphiql`).""")
                        .contact(new Contact().name("Blogging Platform team"))
                        .license(new License().name("For training use")))
                .servers(serversFor(properties))
                .tags(List.of(
                        new Tag().name("Users").description("Create, read, update and delete users"),
                        new Tag().name("Authentication").description("Sign in with username and password"),
                        new Tag().name("Posts").description("Blog posts with paging, sorting, filtering and search"),
                        new Tag().name("Comments").description("Comments on posts"),
                        new Tag().name("Tags").description("Tags that group posts"),
                        new Tag().name("Reviews").description("1-5 star reviews of posts (one per user per post)"),
                        new Tag().name("Reports").description("Analytics for the admin dashboard"),
                        new Tag().name("Algorithms").description("Demos of searching and sorting algorithms, with timings"),
                        new Tag().name("System").description("Application status")));
    }

    /**
     * The "Servers" drop-down of Swagger UI: one entry per profile (dev, test, prod) from the
     * {@code blog.servers} properties. The profile that is running now comes first, so it is the one
     * selected by default, and its description is marked "(active)".
     */
    static List<Server> serversFor(AppProperties properties) {
        String active = properties.environment();
        return properties.servers().stream()
                .sorted(Comparator.comparing((AppProperties.ApiServer s) -> !s.profile().equals(active)))  // active first
                .map(s -> new Server()
                        .url(s.url())
                        .description(s.profile() + " - " + s.description()
                                + (s.profile().equals(active) ? " (active)" : "")))
                .toList();
    }
}
