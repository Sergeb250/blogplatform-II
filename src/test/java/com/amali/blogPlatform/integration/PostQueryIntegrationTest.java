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

/**
 * Phase 4: pagination, sorting, filtering and searching through query parameters.
 * Test data (data.sql): post 1 "Getting started with Spring Boot" (alice, Java+Spring, rating 4.5),
 * post 2 "JDBC without JPA" (alice, Java+Database, rating 3), post 3 "Draft notes" (admin, DRAFT, no rating).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PostQueryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void paginationSplitsResultsIntoPages() throws Exception {
        mockMvc.perform(get("/api/posts?page=0&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(false));

        mockMvc.perform(get("/api/posts?page=1&size=2"))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void defaultOrderIsNewestFirst() throws Exception {
        mockMvc.perform(get("/api/posts?sortBy=created_at&direction=DESC"))
                .andExpect(jsonPath("$.data.content[0].id").value(3))
                .andExpect(jsonPath("$.data.content[2].id").value(1));
    }

    @Test
    void sortByTitleAscending() throws Exception {
        mockMvc.perform(get("/api/posts?sortBy=title&direction=ASC"))
                .andExpect(jsonPath("$.data.content[0].title").value("Draft notes"))
                .andExpect(jsonPath("$.data.content[1].title").value("Getting started with Spring Boot"))
                .andExpect(jsonPath("$.data.content[2].title").value("JDBC without JPA"));
    }

    @Test
    void sortByRatingHighestFirst() throws Exception {
        mockMvc.perform(get("/api/posts?sortBy=rating&direction=DESC"))
                .andExpect(jsonPath("$.data.content[0].averageRating").value(4.5))
                .andExpect(jsonPath("$.data.content[1].averageRating").value(3.0));
    }

    @Test
    void filterByAuthorTagAndStatus() throws Exception {
        mockMvc.perform(get("/api/posts?authorId=2"))
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mockMvc.perform(get("/api/posts?tag=Spring"))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(1));
        mockMvc.perform(get("/api/posts?status=DRAFT"))
                .andExpect(jsonPath("$.data.content[0].title").value("Draft notes"));
        mockMvc.perform(get("/api/posts?authorId=2&tag=Database"))
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(2));
    }

    @Test
    void keywordSearchLooksInTitleAndContent() throws Exception {
        mockMvc.perform(get("/api/posts/search?keyword=JdbcTemplate"))   // only in the content of post 2
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].id").value(2));
        mockMvc.perform(get("/api/posts/search?keyword=nothing-matches-this"))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void invalidParametersAreRejectedWith400() throws Exception {
        mockMvc.perform(get("/api/posts?sortBy=password_hash"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("sortBy must be one of: created_at, title, rating (was 'password_hash')"));
        mockMvc.perform(get("/api/posts?direction=SIDEWAYS")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/posts?size=0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/posts?page=-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/posts/search?keyword=")).andExpect(status().isBadRequest());
    }

    @Test
    void algorithmDemos() throws Exception {
        mockMvc.perform(get("/api/algorithms/sort?sortBy=title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.itemCount").value(3))
                .andExpect(jsonPath("$.data.firstTitles[0]").value("Draft notes"));
        mockMvc.perform(get("/api/algorithms/search?algorithm=binary&title=jdbc without jpa"))
                .andExpect(jsonPath("$.data.found").value(true))
                .andExpect(jsonPath("$.data.post.id").value(2));
        mockMvc.perform(get("/api/algorithms/search?algorithm=linear&title=missing"))
                .andExpect(jsonPath("$.data.found").value(false))
                .andExpect(jsonPath("$.data.comparisons").value(3));
    }
}
