# Blogging Platform – Implementation Phases

## Project Overview
This phase of the Blogging Platform turns the existing database foundation (the `bloggingPlatform` MySQL database) into a **Spring Boot web application** with **REST and GraphQL APIs**. It applies validation, exception handling, AOP and OpenAPI documentation. The APIs manage **users, posts, comments, tags and reviews** through a layered architecture (`Controller → Service → Repository`). Algorithms are applied to sorting, searching and pagination.

## Key Technical Decisions
| Decision | Choice |
|---|---|
| Framework | Spring Boot **3.5.7** (Web, JDBC, Validation, AOP, GraphQL, Springdoc OpenAPI 2.8) |
| Language | **Java 21** |
| Database access | **Spring JDBC (`JdbcTemplate`) only**: no JPA, no Hibernate, no `@Entity`, no `JpaRepository` |
| Database | Existing MySQL `bloggingPlatform` (`users`, `posts`, `tags`, `post_tags`, `comments`, `reviews`); H2 for tests |
| Project | One Maven project, base package `com.amali.blogPlatform` (Spring Boot only; no desktop UI) |
| Dependency injection | Constructor injection everywhere |
| Interaction / testing | Postman, Swagger UI, GraphiQL, JUnit 5 + Mockito + MockMvc |

## Status
| Phase | Epic / User story | Points | Status |
|---|---|---|---|
| 1. Project Setup and Configuration | Epic 1 · US 1.1 | 15 | ✅ Done |
| 2. Database Integration and Layered Architecture | Epic 2 · US 2.1 | – | ✅ Done |
| 3. REST API CRUD Development | Epic 2 · US 2.1 | 20 | ✅ Done |
| 4. Searching, Sorting, Filtering and Pagination | Epic 2 · US 2.2 | part of 15 | ✅ Done |
| 5. Validation and Exception Handling | Epic 3 · US 3.1 | part of 20 | ✅ Done |
| 6. OpenAPI Documentation | Epic 3 · US 3.1 | part of 20 | ✅ Done |
| 7. GraphQL Integration | Epic 4 · US 4.1 | 15 | ✅ Done |
| 8. AOP and Performance Monitoring | Epic 5 · US 5.1 | part of 15 | ✅ Done |
| 9. Testing, Performance Report and Final Documentation | All epics | 15 | ✅ Done |

---

## Phase 1: Project Setup and Configuration
**Epic 1 · User Story 1.1**
> As a developer, I want to configure and structure a Spring Boot project so that it runs efficiently across multiple environments.

**Acceptance criteria:** Spring Boot project initialized with required dependencies · profiles for `dev`, `test`, `prod` · constructor-based DI used consistently.

**Delivered**
- `pom.xml`: Spring Boot 3.5.7 parent, Java 21. Dependencies: web, jdbc, validation, aop, graphql, springdoc, mysql, jbcrypt, test, graphql-test, h2.
- `BloggingApplication` (`@SpringBootApplication`, `@ConfigurationPropertiesScan`) and `config/AppProperties` (typed `blog.*` settings).
- `application.properties` plus `application-dev/-test/-prod.properties`. Credentials come from environment variables.
- The package structure of the brief; constructor injection only.
- `GET /api/system/info` shows the active profile.

**Expected outputs:** working app on Java 21 · all dependencies · three profiles · clean packages · constructor injection.
**Acceptance test:** `./mvnw spring-boot:run` → `Started BloggingApplication`, `Tomcat started on port 8080`. ✅

---

## Phase 2: Database Integration and Layered Architecture
**Epic 2 · User Story 2.1**
> As an administrator, I want to manage users, posts, comments, and tags through REST endpoints so that I can maintain the blogging platform.

**Delivered**
- `config/DatabaseConfig`: `JdbcTemplate` with a query timeout, `NamedParameterJdbcTemplate`, `@EnableTransactionManagement`, and a startup connection check.
- `model/`: records `User`, `Post`, `Comment`, `Tag`, `Review` and enums `Role`, `PostStatus`.
- `repository/`: one class per table, each with a `RowMapper`. They use `query`, `queryForObject`, `update`, `GeneratedKeyHolder` and `batchUpdate`. Tags are loaded for many posts at once with `IN (:ids)`, so there is no N+1.
- `service/` (`@Transactional`), `controller/` and response DTOs. The password hash is never returned.

**Expected outputs:** connection to the existing DB · models for the 5 entities · JDBC repositories · services · controllers.
**Acceptance test:** `GET /api/posts` returns the posts of the real database. ✅ (Connected to MySQL 8.0.46; 17 posts, 12 users, 8 tags.)

---

## Phase 3: REST API CRUD Development
**Epic 2 · User Story 2.1**

**Acceptance criteria:** CRUD following REST conventions · responses with `status`, `message`, `data` · clean layered communication.

**Delivered**
- CRUD for users, posts, comments, tags and reviews, plus `POST /api/auth/login` (username or email + BCrypt).
- 10 request DTOs and `ApiResponse<T>`.
- Status codes: 201 with `Location`, 200, 204, 401, 404, 409.
- Business rules in the services:
  - unique username, email and tag name;
  - an author cannot reuse a title;
  - one review per user per post;
  - a post and its tags are saved in one transaction.
- Postman collection `docs/postman/blog-platform.postman_collection.json` (87 requests in 13 folders, REST and GraphQL, cleans up after itself).

**Acceptance test:** create, read, update and delete a user, then the same for posts, comments, tags and reviews. ✅ (Also verified on real MySQL with full cleanup.)

---

## Phase 4: Searching, Sorting, Filtering and Pagination
**Epic 2 · User Story 2.2**
> As a reader, I want to view, sort, and filter blog posts so that I can find interesting articles easily.

**Acceptance criteria:** pagination, sorting, filtering through query parameters · efficient searching with algorithms or indexed fields · performance documented and analysed.

**Delivered**
- `GET /api/posts?page&size&sortBy&direction&authorId&tag&status&keyword` builds dynamic SQL with `LIMIT ? OFFSET ?` and returns a `PageResponse` with totals.
- Sort columns come from an **allowed list** (`PostSortField`). Invalid values give 400.
- `GET /api/posts/search?keyword=` uses `LIKE` with escaped wildcards.
- Indexes: `db/indexes.sql`, including the new `idx_posts_created_at`.
- `algorithms/`: LinearSearch, BinarySearch and MergeSort, each with unit tests (QuickSort was removed later to keep one sort). Demo endpoints are under `/api/algorithms`.
- `QueryBenchmarkTest` runs on 100,000 posts, without and with indexes. Results are in `docs/performance/query-benchmark.md`.
- Reports: `/api/reports/summary`, `/posts-per-tag`, `/top-rated`, `/most-commented`.

**Acceptance tests:** `GET /api/posts?page=0&size=5` · `?sortBy=created_at&direction=DESC` · `?authorId=3` · `/api/posts/search?keyword=Java`. ✅ Indexes made the main queries **107–135× faster**.

---

## Phase 5: Validation and Exception Handling
**Epic 3 · User Story 3.1**
> As a developer, I want to validate and document all API endpoints so that they remain consistent and reliable.

**Delivered**
- Bean Validation on every request DTO: `@NotBlank`, `@Size`, `@Email`, `@Pattern`, `@NotNull`, `@Positive`, `@Min/@Max`. `@Valid` is used in the controllers, and `page` and `size` are limited.
- `@UniqueUsername` and `@UniqueEmail` validators backed by `UserRepository`. A failure gives 409.
- Custom exceptions: the `…NotFoundException` family, the `Duplicate…Exception` family, `InvalidParameterException`, `InvalidCredentialsException`.
- `GlobalExceptionHandler` with 400 (field map), 401, 404, 405, 409 and 500 (generic message). Every error uses the `ApiResponse` shape.

**Acceptance tests:**
- invalid email → 400
- existing email → 409
- post without title → 400
- comment without content → 400
- unknown author → 404

✅ In every case nothing is saved.

---

## Phase 6: OpenAPI Documentation
**Epic 3 · User Story 3.1**

**Delivered**
- `config/OpenApiConfig`: title, version, description and tag groups.
- `@Tag`, `@Operation`, `@ApiResponses` and `@Parameter` on every endpoint, and `@Schema` descriptions and examples on the DTOs.
- `OpenApiDocumentationTest` checks the generated document.

**Expected outputs:** Swagger UI at `http://localhost:8080/swagger-ui/index.html` · every endpoint documented with status codes and examples. ✅

---

## Phase 7: GraphQL Integration
**Epic 4 · User Story 4.1**
> As a frontend developer, I want to fetch data using GraphQL queries and mutations so that I can retrieve only the data needed for the interface.

**Delivered**
- `graphql/schema.graphqls` with the types User, Post, Comment, Tag, Review and PostPage.
- Queries: `posts`, `postPage`, `post`, `searchPosts`, `users`, `user`, `tags`, `tag`, `comments`, `comment`, `reviews`, `review`.
- Create, update and delete mutations for every entity.
- 5 GraphQL controllers using `@QueryMapping`, `@MutationMapping`, `@SchemaMapping`, `@BatchMapping` (`Post.author` without N+1) and `@Argument`.
- The controllers **reuse the REST services**.
- `RequestValidator` gives GraphQL the same validation as REST. `GraphQlExceptionHandler` maps errors to NOT_FOUND / BAD_REQUEST / UNAUTHORIZED.

**Acceptance tests:** all posts · one post · post with author and comments · create · update · delete · users and tags. ✅ (`GraphQlIntegrationTest`; GraphiQL at `/graphiql`; REST and GraphQL run side by side.)

---

## Phase 8: AOP and Performance Monitoring
**Epic 5 · User Story 5.1**
> As a developer, I want to use AOP for logging and monitoring so that common concerns are handled centrally.

**Delivered**
- `aspect/LoggingAspect`, applied to every service method:
  - `@Before`: method and arguments, with passwords masked;
  - `@After`;
  - `@AfterThrowing`: logs the error and rethrows it;
  - `@Around`: `System.nanoTime()` timing, with a "SLOW" warning above a configurable threshold.
- `aspect/PerformanceMonitor` keeps statistics per method, shown at `GET /api/metrics/methods`.

**Acceptance test:** before/after/around advice logged · execution time recorded · errors not swallowed · applied to CRUD and analytics. ✅ (`LoggingAspectTest`)

---

## Phase 9: Testing, Performance Report and Final Documentation
**All epics**

**Delivered**
- **103 automated tests**, all passing:
  - unit tests (Mockito);
  - repository tests (`@JdbcTest` on H2);
  - web layer test (`@WebMvcTest`);
  - integration tests (`@SpringBootTest` + MockMvc);
  - GraphQL, AOP and OpenAPI tests.
- Each test class covers both success and failure cases (201/200/204/400/401/404/405/409).
- `docs/performance-report.md` with the 10 sections of the brief. It covers REST vs GraphQL, searching with and without indexes, pagination, sorting, and query execution times, all measured.
- `docs/performance/rest-vs-graphql.sh` and `QueryBenchmarkTest` repeat the measurements.
- `README.md` covers every point the brief lists.

| Test from the brief | Expected | Result |
|---|---|---|
| Create valid post | 201 Created | ✅ |
| Create post with empty title | 400 Bad Request | ✅ |
| Retrieve existing post | 200 OK | ✅ |
| Retrieve nonexistent post | 404 Not Found | ✅ |
| Create duplicate username | 409 Conflict | ✅ |
| Delete existing post | 204 No Content | ✅ |

---

## Deliverables Checklist
| Deliverable | Where |
|---|---|
| Spring Boot web application (REST + GraphQL, JDBC) | `src/main/java/com/amali/blogPlatform` |
| Validation and exception handling | `dto/request`, `validation`, `exception` |
| API documentation | Swagger UI `/swagger-ui/index.html`, `config/OpenApiConfig` |
| AOP implementation | `aspect/LoggingAspect`, `aspect/PerformanceMonitor` |
| GraphQL schema and queries | `resources/graphql/schema.graphqls`, `graphql/` |
| Performance report | `docs/performance-report.md` |
| README | `README.md` |

## Evaluation Mapping
| Category | Points | Phases |
|---|---|---|
| Spring Boot Configuration & IoC | 15 | 1, 2 |
| REST API Development | 20 | 2, 3 |
| Validation & Documentation | 20 | 5, 6 |
| GraphQL & Data Integration | 15 | 7 |
| AOP & Algorithmic Optimization | 15 | 4, 8 |
| Code Quality & Reporting | 15 | 9 |
| **Total** | **100** | |
