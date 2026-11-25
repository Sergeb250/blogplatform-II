package com.amali.blogPlatform.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Create → read → update → delete for every resource, through real HTTP requests (MockMvc) down to
 * the H2 database. {@code @Transactional} rolls every test back, so the tests do not affect each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CrudEndpointsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /** Sends a JSON body and returns the id from {@code data.id} of the answer. */
    private long createAndGetId(String url, String json) throws Exception {
        MvcResult result = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    @Test
    void userCrud() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"carol","email":"carol@blog.com","password":"secret123"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.role").value("READER"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());

        long id = createAndGetId("/api/users", """
                {"username":"dave","email":"dave@blog.com","password":"secret123","role":"AUTHOR"}""");

        mockMvc.perform(get("/api/users/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("dave"));

        mockMvc.perform(put("/api/users/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"david","email":"david@blog.com"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("david"))
                .andExpect(jsonPath("$.data.role").value("AUTHOR"));

        mockMvc.perform(delete("/api/users/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void duplicateUsernameAndEmailAreConflicts() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"alice","email":"other@blog.com","password":"secret123"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Duplicate value"))
                .andExpect(jsonPath("$.data.username").value("Username 'alice' is already taken"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"newname","email":"alice@blog.com","password":"secret123"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.email").value("Email 'alice@blog.com' is already registered"));
    }

    @Test
    void postCrudWithTags() throws Exception {
        long id = createAndGetId("/api/posts", """
                {"title":"Phase 3 post","content":"Created through the REST API","authorId":2,"tagIds":[1,3]}""");

        mockMvc.perform(get("/api/posts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.authorUsername").value("alice"))
                .andExpect(jsonPath("$.data.tags", hasSize(2)));

        mockMvc.perform(put("/api/posts/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Phase 3 post (edited)","content":"New text that is long enough","status":"PUBLISHED","tagIds":[2]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Phase 3 post (edited)"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.data.tags[0].name").value("Spring"));

        mockMvc.perform(delete("/api/posts/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/posts/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void postForUnknownAuthorIsNotFound() throws Exception {
        mockMvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Orphan post","content":"A post whose author does not exist","authorId":999}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User with id 999 not found"));
    }

    @Test
    void commentCrud() throws Exception {
        long id = createAndGetId("/api/posts/2/comments", """
                {"userId":1,"content":"A new comment"}""");

        mockMvc.perform(get("/api/comments/" + id))
                .andExpect(jsonPath("$.data.username").value("admin"));
        mockMvc.perform(put("/api/comments/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"An edited comment"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("An edited comment"));
        mockMvc.perform(delete("/api/comments/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/comments/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void tagCrudAndDuplicateName() throws Exception {
        long id = createAndGetId("/api/tags", """
                {"name":"GraphQL"}""");

        mockMvc.perform(put("/api/tags/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"GraphQL API"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("GraphQL API"));

        mockMvc.perform(post("/api/tags").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Java"}"""))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/tags/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/tags/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void reviewCrudAndOneReviewPerUser() throws Exception {
        long id = createAndGetId("/api/posts/3/reviews", """
                {"userId":3,"rating":4,"reviewText":"Nice draft"}""");

        mockMvc.perform(post("/api/posts/3/reviews").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":3,"rating":5}"""))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/reviews/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"rating":2,"reviewText":"Changed my mind"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(2));

        mockMvc.perform(delete("/api/reviews/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/reviews/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void loginChecksThePassword() throws Exception {
        createAndGetId("/api/users", """
                {"username":"erin","email":"erin@blog.com","password":"secret123"}""");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"erin","password":"secret123"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("erin"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"erin","password":"wrong"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }
}
