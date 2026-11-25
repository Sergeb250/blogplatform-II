package com.amali.blogPlatform.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phase 5: invalid requests are rejected with a clear error and nothing is saved. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    // ------------------------------------------------ the five acceptance tests of the brief

    @Test
    void userWithInvalidEmail() throws Exception {
        postJson("/api/users", """
                {"username":"carol","email":"not-an-email","password":"secret123"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.email").value("Email must be a valid email address"));
    }

    @Test
    void userWithExistingEmail() throws Exception {
        postJson("/api/users", """
                {"username":"carol","email":"alice@blog.com","password":"secret123"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.email").value("Email 'alice@blog.com' is already registered"));
    }

    @Test
    void postWithoutTitle() throws Exception {
        postJson("/api/posts", """
                {"content":"Content that is long enough to pass","authorId":2}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.title").value("Title is required"));
    }

    @Test
    void commentWithoutContent() throws Exception {
        postJson("/api/posts/1/comments", """
                {"userId":3}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.content").value("Comment content is required"));
    }

    @Test
    void postWithNonexistentAuthor() throws Exception {
        postJson("/api/posts", """
                {"title":"Valid title","content":"Content that is long enough to pass","authorId":999}""")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User with id 999 not found"));
    }

    // ------------------------------------------------ more rules

    @Test
    void severalFieldErrorsAreReportedTogether() throws Exception {
        postJson("/api/users", """
                {"username":"a!","email":"","password":"short"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.username").exists())
                .andExpect(jsonPath("$.data.email").value("Email is required"))
                .andExpect(jsonPath("$.data.password").value("Password must be 6 to 64 characters"));
    }

    @Test
    void ratingMustBeBetweenOneAndFive() throws Exception {
        postJson("/api/posts/3/reviews", """
                {"userId":3,"rating":9}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.rating").value("Rating must be between 1 and 5"));
    }

    @Test
    void contentTooShortAndTagIdNotPositive() throws Exception {
        postJson("/api/posts", """
                {"title":"Valid title","content":"too short","authorId":2,"tagIds":[0]}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.content").value("Content must be at least 20 characters"))
                .andExpect(jsonPath("$.data['tagIds[0]']").exists());
    }

    @Test
    void invalidJsonAndWrongEnumValue() throws Exception {
        postJson("/api/tags", "{ this is not json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
        postJson("/api/posts", """
                {"title":"Valid title","content":"Content that is long enough","authorId":2,"status":"LATER"}""")
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidParameters() throws Exception {
        mockMvc.perform(get("/api/posts/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));
        mockMvc.perform(get("/api/posts?size=500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.data.size").value("size must be at most 100"));
        mockMvc.perform(get("/api/posts/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter 'keyword'"));
    }

    @Test
    void unknownUrlAndUnsupportedMethod() throws Exception {
        mockMvc.perform(get("/api/postz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(patch("/api/posts/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void wrongValueTypeInBodyNamesTheField() throws Exception {
        postJson("/api/users", """
                {"username":"carol","email":"carol@blog.com","password":"secret123","role":"KING"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'KING' for field 'role' (allowed: [ADMIN, AUTHOR, READER])"));
        postJson("/api/posts", """
                {"title":"Valid title","content":"Content long enough for validation.","authorId":"abc"}""")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for field 'authorId'"));
        postJson("/api/posts", "{bad json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body is missing or is not valid JSON"));
    }

    @Test
    void nothingWasSavedAfterRejectedRequests() throws Exception {
        postJson("/api/users", """
                {"username":"ghost","email":"bad","password":"secret123"}""")
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/users"))
                .andExpect(jsonPath("$.data.length()").value(3));    // still only the 3 test users
    }
}
