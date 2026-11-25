package com.amali.blogPlatform.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Soft delete: a DELETE request hides the record (deleted_at gets a date) but the row stays in the
 * table. Also the unique post title rule. Test data (data.sql): admin=1, alice=2, bob=3;
 * posts 1 and 2 by alice, post 3 by admin; tags Java=1, Spring=2, Database=3.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SoftDeleteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
                               String json) throws Exception {
        return mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private long rowsIn(String table, String where) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Long.class);
    }

    // ------------------------------------------------------------------ soft delete

    @Test
    void deletedPostIsHiddenButKeptWithItsCommentsAndReviews() throws Exception {
        mockMvc.perform(delete("/api/posts/1")).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/posts/1")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/posts")).andExpect(jsonPath("$.data.content[*].id", not(hasItem(1))));
        mockMvc.perform(get("/api/posts/1/comments")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(jsonPath("$.data.posts").value(2))
                .andExpect(jsonPath("$.data.comments").value(1))           // only the comment on post 2
                .andExpect(jsonPath("$.data.reviews").value(1));

        // nothing was physically removed: the rows are only marked
        assertThat(rowsIn("posts", "post_id = 1 AND deleted_at IS NOT NULL")).isEqualTo(1);
        assertThat(rowsIn("comments", "post_id = 1 AND deleted_at IS NOT NULL")).isEqualTo(2);
        assertThat(rowsIn("reviews", "post_id = 1 AND deleted_at IS NOT NULL")).isEqualTo(2);
    }

    @Test
    void deletingTwiceIsNotFound() throws Exception {
        mockMvc.perform(delete("/api/comments/1")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/comments/1")).andExpect(status().isNotFound());
        mockMvc.perform(put("/api/comments/1").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"edit\"}"))
                .andExpect(status().isNotFound());                          // a deleted comment cannot be edited
    }

    @Test
    void deletedUserTakesTheirContentAlongAndKeepsTheUsernameReserved() throws Exception {
        mockMvc.perform(delete("/api/users/2")).andExpect(status().isNoContent());      // alice

        mockMvc.perform(get("/api/users/2")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/users")).andExpect(jsonPath("$.data", hasSize(2)));
        mockMvc.perform(get("/api/posts")).andExpect(jsonPath("$.data.content[*].id", contains(3)));  // only admin's post
        mockMvc.perform(get("/api/comments")).andExpect(jsonPath("$.data", hasSize(0)));    // all were on alice's posts
        assertThat(rowsIn("users", "user_id = 2 AND deleted_at IS NOT NULL")).isEqualTo(1);

        // the deleted user's name and email stay reserved
        send(post("/api/users"), """
                {"username":"alice","email":"new-alice@blog.com","password":"secret123"}""")
                .andExpect(status().isConflict());
        // a deleted user cannot write a new post
        send(post("/api/posts"), """
                {"title":"Ghost writer","content":"Written by a user who was deleted.","authorId":2}""")
                .andExpect(status().isNotFound());
    }

    @Test
    void deletedUserCannotLogIn() throws Exception {
        String body = send(post("/api/users"), """
                {"username":"carol","email":"carol@blog.com","password":"secret123"}""")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
        send(post("/api/auth/login"), "{\"username\":\"carol\",\"password\":\"secret123\"}").andExpect(status().isOk());

        mockMvc.perform(delete("/api/users/" + id)).andExpect(status().isNoContent());

        send(post("/api/auth/login"), "{\"username\":\"carol\",\"password\":\"secret123\"}")
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletedTagDisappearsFromPostsAndCreatingItAgainRestoresIt() throws Exception {
        mockMvc.perform(delete("/api/tags/1")).andExpect(status().isNoContent());           // Java

        mockMvc.perform(get("/api/posts/1")).andExpect(jsonPath("$.data.tags[*].name", contains("Spring")));
        mockMvc.perform(get("/api/posts?tag=Java")).andExpect(jsonPath("$.data.totalElements").value(0));
        send(post("/api/posts"), """
                {"title":"Uses a deleted tag","content":"This post tries to use a deleted tag.","authorId":1,"tagIds":[1]}""")
                .andExpect(status().isNotFound());

        send(post("/api/tags"), "{\"name\":\"Java\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1));                                 // the same tag is back
        mockMvc.perform(get("/api/posts/1")).andExpect(jsonPath("$.data.tags[*].name", contains("Java", "Spring")));
    }

    @Test
    void reviewingAgainAfterDeletingRestoresTheReview() throws Exception {
        mockMvc.perform(delete("/api/reviews/1")).andExpect(status().isNoContent());        // bob's review of post 1

        send(post("/api/posts/1/reviews"), "{\"userId\":3,\"rating\":2,\"reviewText\":\"Changed my mind\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.rating").value(2));
        mockMvc.perform(get("/api/posts/1")).andExpect(jsonPath("$.data.averageRating").value(3.0));
    }

    // ------------------------------------------------------------------ unique post title

    @Test
    void twoPostsCannotHaveTheSameTitle() throws Exception {
        // admin (1) uses the title of alice's post 2: another author, same title
        send(post("/api/posts"), """
                {"title":"JDBC without JPA","content":"Another post with exactly the same title.","authorId":1}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.title").value("A post titled 'JDBC without JPA' already exists"));
        // upper/lower case does not make it different
        send(post("/api/posts"), """
                {"title":"jdbc WITHOUT jpa","content":"Another post with the same title in other case.","authorId":1}""")
                .andExpect(status().isConflict());
        // renaming post 3 to an existing title is rejected too
        send(put("/api/posts/3"), """
                {"title":"JDBC without JPA","content":"Content long enough for validation."}""")
                .andExpect(status().isConflict());
        // keeping its own title is fine
        send(put("/api/posts/3"), """
                {"title":"Draft notes","content":"New content, same title as before."}""")
                .andExpect(status().isOk());
    }

    @Test
    void titleOfADeletedPostStaysReserved() throws Exception {
        mockMvc.perform(delete("/api/posts/2")).andExpect(status().isNoContent());
        send(post("/api/posts"), """
                {"title":"JDBC without JPA","content":"Reusing the title of a deleted post.","authorId":1}""")
                .andExpect(status().isConflict());
    }
}
