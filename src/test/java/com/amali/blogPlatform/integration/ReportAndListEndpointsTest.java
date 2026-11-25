package com.amali.blogPlatform.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The analytics reports and the comment/review lists filtered by writer or post author. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportAndListEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void summaryCountsEverything() throws Exception {
        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.users").value(3))
                .andExpect(jsonPath("$.data.authors").value(1))
                .andExpect(jsonPath("$.data.posts").value(3))
                .andExpect(jsonPath("$.data.drafts").value(1))
                .andExpect(jsonPath("$.data.comments").value(3))
                .andExpect(jsonPath("$.data.reviews").value(3))
                .andExpect(jsonPath("$.data.averageRating").value(4.0));
    }

    @Test
    void postsPerTagTopRatedAndMostCommented() throws Exception {
        mockMvc.perform(get("/api/reports/posts-per-tag"))
                .andExpect(jsonPath("$.data[0].tag").value("Java"))
                .andExpect(jsonPath("$.data[0].posts").value(2));
        mockMvc.perform(get("/api/reports/top-rated?limit=5"))
                .andExpect(jsonPath("$.data", hasSize(2)))                     // post 3 has no reviews
                .andExpect(jsonPath("$.data[0].id").value(1));
        mockMvc.perform(get("/api/reports/most-commented?limit=1"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].commentCount").value(2));
        mockMvc.perform(get("/api/reports/top-rated?limit=0")).andExpect(status().isBadRequest());
    }

    @Test
    void commentsAndReviewsCanBeListedByWriterOrPostAuthor() throws Exception {
        mockMvc.perform(get("/api/comments")).andExpect(jsonPath("$.data", hasSize(3)));
        mockMvc.perform(get("/api/comments?userId=3"))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].postTitle").value("JDBC without JPA"));
        mockMvc.perform(get("/api/comments?postAuthorId=2")).andExpect(jsonPath("$.data", hasSize(3)));
        mockMvc.perform(get("/api/reviews?userId=1")).andExpect(jsonPath("$.data", hasSize(1)));
        mockMvc.perform(get("/api/reviews?postAuthorId=1")).andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void loginAcceptsUsernameOrEmail() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"frank\",\"email\":\"frank@blog.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"frank@blog.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("frank"));
    }
}
