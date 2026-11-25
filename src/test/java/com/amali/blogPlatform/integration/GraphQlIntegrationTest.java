package com.amali.blogPlatform.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.test.tester.GraphQlTester;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 7: GraphQL queries and mutations through the GraphQL engine (schema, controllers, validation,
 * error handler), down to the H2 database. The last test also calls the HTTP endpoint POST /graphql.
 * Same test data as the REST tests (data.sql).
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureGraphQlTester
@ActiveProfiles("test")
@Transactional
class GraphQlIntegrationTest {

    @Autowired
    private GraphQlTester graphQlTester;

    @Autowired
    private MockMvc mockMvc;

    // ------------------------------------------------ the 7 acceptance tests of the brief

    @Test
    void retrieveAllPosts() {
        graphQlTester.document("query { posts { id title } }")
                .execute()
                .path("posts").entityList(Object.class).hasSize(3)
                .path("posts[0].title").entity(String.class).isEqualTo("Draft notes");
    }

    @Test
    void retrieveOnePost() {
        graphQlTester.document("""
                        query { post(id: "1") { title content averageRating tags { name } } }""")
                .execute()
                .path("post.title").entity(String.class).isEqualTo("Getting started with Spring Boot")
                .path("post.averageRating").entity(Double.class).isEqualTo(4.5)
                .path("post.tags[*].name").entityList(String.class).containsExactly("Java", "Spring");
    }

    @Test
    void retrievePostWithAuthorAndComments() {
        graphQlTester.document("""
                        query {
                          post(id: "1") {
                            title
                            author { username role }
                            comments { username content }
                          }
                        }""")
                .execute()
                .path("post.author.username").entity(String.class).isEqualTo("alice")
                .path("post.author.role").entity(String.class).isEqualTo("AUTHOR")
                .path("post.comments").entityList(Object.class).hasSize(2)
                .path("post.comments[0].username").entity(String.class).isEqualTo("bob");
    }

    @Test
    void createUpdateAndDeleteAPost() {
        String id = graphQlTester.document("""
                        mutation {
                          createPost(title: "Spring Boot", content: "Learning GraphQL with Spring Boot today",
                                     authorId: "1", tagIds: ["2"]) {
                            id title status author { username } tags { name }
                          }
                        }""")
                .execute()
                .path("createPost.title").entity(String.class).isEqualTo("Spring Boot")
                .path("createPost.status").entity(String.class).isEqualTo("DRAFT")
                .path("createPost.author.username").entity(String.class).isEqualTo("admin")
                .path("createPost.tags[0].name").entity(String.class).isEqualTo("Spring")
                .path("createPost.id").entity(String.class).get();

        graphQlTester.document("""
                        mutation($id: ID!) {
                          updatePost(id: $id, title: "Spring Boot GraphQL",
                                     content: "Updated content that is long enough", status: PUBLISHED) {
                            title status
                          }
                        }""")
                .variable("id", id)
                .execute()
                .path("updatePost.title").entity(String.class).isEqualTo("Spring Boot GraphQL")
                .path("updatePost.status").entity(String.class).isEqualTo("PUBLISHED");

        graphQlTester.document("mutation($id: ID!) { deletePost(id: $id) }")
                .variable("id", id)
                .execute()
                .path("deletePost").entity(Boolean.class).isEqualTo(true);

        graphQlTester.document("query($id: ID!) { post(id: $id) { title } }")
                .variable("id", id)
                .execute()
                .errors().satisfy(errors -> assertThat(errors.get(0).getErrorType()).isEqualTo(ErrorType.NOT_FOUND));
    }

    @Test
    void retrieveUsersAndTags() {
        graphQlTester.document("query { users { username email } tags { name } }")
                .execute()
                .path("users").entityList(Object.class).hasSize(3)
                .path("users[1].username").entity(String.class).isEqualTo("alice")
                .path("tags[*].name").entityList(String.class).containsExactly("Database", "Java", "Spring");
    }

    // ------------------------------------------------ more

    @Test
    void clientGetsOnlyTheFieldsItAskedFor() {
        graphQlTester.document("query { user(id: \"2\") { username } }")
                .execute()
                .path("user.username").entity(String.class).isEqualTo("alice")
                .path("user.email").pathDoesNotExist();
    }

    @Test
    void pagingSortingAndFilteringWorkLikeRest() {
        graphQlTester.document("""
                        query { postPage(size: 2, sortBy: "title", direction: "ASC", authorId: "2") {
                                  content { title } totalElements totalPages } }""")
                .execute()
                .path("postPage.totalElements").entity(Integer.class).isEqualTo(2)
                .path("postPage.content[0].title").entity(String.class).isEqualTo("Getting started with Spring Boot");
        graphQlTester.document("query { searchPosts(keyword: \"JdbcTemplate\") { content { id } } }")
                .execute()
                .path("searchPosts.content[0].id").entity(String.class).isEqualTo("2");
    }

    @Test
    void userPostsAndCommentTagReviewMutations() {
        graphQlTester.document("query { user(id: \"2\") { posts { title } } }")
                .execute()
                .path("user.posts").entityList(Object.class).hasSize(2);
        graphQlTester.document("mutation { addComment(postId: \"2\", userId: \"1\", content: \"From GraphQL\") { username } }")
                .execute()
                .path("addComment.username").entity(String.class).isEqualTo("admin");
        graphQlTester.document("mutation { createTag(name: \"GraphQL\") { name } }")
                .execute()
                .path("createTag.name").entity(String.class).isEqualTo("GraphQL");
        graphQlTester.document("mutation { createReview(postId: \"3\", userId: \"3\", rating: 4) { rating username } }")
                .execute()
                .path("createReview.rating").entity(Integer.class).isEqualTo(4);
    }

    @Test
    void validationErrorsAreReportedPerField() {
        graphQlTester.document("""
                        mutation { createUser(username: "x", email: "not-an-email", password: "secret123") { id } }""")
                .execute()
                .errors().satisfy(errors -> {
                    assertThat(errors.get(0).getMessage()).isEqualTo("Validation failed");
                    assertThat(errors.get(0).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    assertThat(errors.get(0).getExtensions().get("fieldErrors").toString())
                            .contains("email=Email must be a valid email address")
                            .contains("username=Username must be 3 to 50 characters");
                });
    }

    @Test
    void duplicateAndNotFoundErrors() {
        graphQlTester.document("mutation { createTag(name: \"Java\") { id } }")
                .execute()
                .errors().satisfy(errors -> assertThat(errors.get(0).getExtensions()).containsEntry("code", "CONFLICT"));
        graphQlTester.document("mutation { createPost(title: \"Orphan post\", content: \"A post without an author\", authorId: \"999\") { id } }")
                .execute()
                .errors().satisfy(errors -> {
                    assertThat(errors.get(0).getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
                    assertThat(errors.get(0).getMessage()).isEqualTo("User with id 999 not found");
                });
    }

    /** Bug found with Postman: an empty id ("{{postId}}" not set yet) used to crash with INTERNAL_ERROR. */
    @Test
    void emptyOrWrongIdsAreBadRequestsNotCrashes() {
        assertBadRequest("query { post(id: \"\") { id } }", "id is required and must be a number, e.g. 1");
        assertBadRequest("query { comments(postId: \"\") { id } }", "postId is required and must be a number, e.g. 1");
        assertBadRequest("mutation { deleteUser(id: \"\") }", "id is required and must be a number, e.g. 1");
        assertBadRequest("query { post(id: \"-1\") { id } }", "id must be a positive number (was -1)");
        assertBadRequest("query { post(id: \"abc\") { id } }", "Invalid value 'abc' for argument 'id'");
        graphQlTester.document("query($id: ID!) { review(id: $id) { id } }")
                .variable("id", "")
                .execute()
                .errors().satisfy(errors -> assertThat(errors.get(0).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST));
    }

    private void assertBadRequest(String document, String expectedMessage) {
        graphQlTester.document(document)
                .execute()
                .errors().satisfy(errors -> {
                    assertThat(errors.get(0).getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    assertThat(errors.get(0).getMessage()).isEqualTo(expectedMessage);
                });
    }

    @Test
    void restAndGraphQlWorkSideBySide() throws Exception {
        mockMvc.perform(get("/api/posts/1")).andExpect(status().isOk());
        graphQlTester.document("query { post(id: \"1\") { id } }").execute()
                .path("post.id").entity(String.class).isEqualTo("1");
        mockMvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"{ post(id: \\\"1\\\") { title } }\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.post.title").value("Getting started with Spring Boot"));
        mockMvc.perform(get("/graphiql")).andExpect(status().is3xxRedirection());
    }
}
