package com.amali.blogPlatform.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phase 6: Springdoc generates the OpenAPI document, and Swagger UI is served. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocumentListsEveryResource() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Blogging Platform API"))
                .andExpect(jsonPath("$.info.version").value("1.0.0"))
                .andExpect(jsonPath("$.paths['/api/users']").exists())
                .andExpect(jsonPath("$.paths['/api/posts']").exists())
                .andExpect(jsonPath("$.paths['/api/posts/search']").exists())
                .andExpect(jsonPath("$.paths['/api/posts/{postId}/comments']").exists())
                .andExpect(jsonPath("$.paths['/api/tags/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/reviews/{id}']").exists())
                .andExpect(jsonPath("$.paths['/api/posts'].post.summary").value("Create a blog post"))
                .andExpect(jsonPath("$.paths['/api/posts'].post.responses['201'].description").value("Post created successfully"))
                .andExpect(jsonPath("$.paths['/api/posts'].post.responses['404']").exists())
                .andExpect(jsonPath("$.components.schemas.CreatePostRequest.properties.title.example")
                        .value("Getting started with Spring Boot"));
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    /** The "Servers" drop-down lists every profile; the running one (test, here) comes first. */
    @Test
    void serversListEveryProfileWithTheActiveOneFirst() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.servers.length()").value(3))
                .andExpect(jsonPath("$.servers[0].url").value("http://localhost:8081"))
                .andExpect(jsonPath("$.servers[0].description").value(
                        "test - Test: in-memory H2 database with sample data (active)"))
                .andExpect(jsonPath("$.servers[1].description").value(org.hamcrest.Matchers.startsWith("dev - ")))
                .andExpect(jsonPath("$.servers[2].description").value(org.hamcrest.Matchers.startsWith("prod - ")));
    }
}
