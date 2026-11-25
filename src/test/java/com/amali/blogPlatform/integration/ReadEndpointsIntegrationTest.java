package com.amali.blogPlatform.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Whole stack: controller → service → repository → JdbcTemplate → H2 (test profile). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReadEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getAllPostsReturnsDatabaseRows() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Posts retrieved successfully"))
                .andExpect(jsonPath("$.data.content", hasSize(3)))
                .andExpect(jsonPath("$.data.content[2].title").value("Getting started with Spring Boot"))
                .andExpect(jsonPath("$.data.content[2].authorUsername").value("alice"))
                .andExpect(jsonPath("$.data.content[2].tags[0].name").value("Java"))
                .andExpect(jsonPath("$.data.content[2].averageRating").value(4.5));
    }

    @Test
    void getPostByIdAndMissingPost() throws Exception {
        mockMvc.perform(get("/api/posts/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("JDBC without JPA"));

        mockMvc.perform(get("/api/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Post with id 999 not found"));
    }

    @Test
    void usersNeverExposePasswordHash() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].passwordHash").doesNotExist());
    }

    @Test
    void commentsTagsAndReviewsAreReadable() throws Exception {
        mockMvc.perform(get("/api/posts/1/comments"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(2)));
        mockMvc.perform(get("/api/comments/3"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.postId").value(2));
        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(3)));
        mockMvc.perform(get("/api/posts/1/reviews"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data", hasSize(2)));
        mockMvc.perform(get("/api/reviews/1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.rating").value(5));
        mockMvc.perform(get("/api/posts/999/comments"))
                .andExpect(status().isNotFound());
    }
}
