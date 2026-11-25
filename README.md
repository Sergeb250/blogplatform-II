# Blogging Platform API

A Spring Boot web application that exposes **REST and GraphQL APIs** for users, posts, comments, tags and reviews. It works with the existing `bloggingPlatform` MySQL database through **Spring JDBC (`JdbcTemplate`), not JPA or Hibernate**. It includes:
- validation and central exception handling,
- OpenAPI/Swagger documentation,
- AOP logging and performance monitoring,
- paging, sorting, filtering and search done in SQL, plus searching and sorting algorithm demos.

Phase plan and status: [`phases.md`](phases.md) · Performance report: [`docs/performance-report.md`](docs/performance-report.md)

---

## Technologies
| Area | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5.7: Web, JDBC, Validation, AOP, GraphQL |
| Database | MySQL 8 (`bloggingPlatform`); H2 in-memory database for tests |
| Data access | `JdbcTemplate`, `NamedParameterJdbcTemplate`, `RowMapper`, `GeneratedKeyHolder`, `batchUpdate` |
| API documentation | Springdoc OpenAPI 2.8 + Swagger UI |
| Passwords | BCrypt (jBCrypt) |
| Tests | JUnit 5, Mockito, AssertJ, MockMvc, Spring GraphQL Test, Postman |
| Build | Maven (wrapper included: `mvnw`) |

## Architecture
```
 REST client (Postman, Swagger UI)        GraphQL client (GraphiQL, Altair)
            │  HTTP /api/...                        │  POST /graphql
            ▼                                       ▼
   controller/  (@RestController)          graphql/  (@Controller, @QueryMapping, ...)
            └──────────────┬────────────────────────┘
                           ▼
          service/  (@Service, @Transactional)  ◄── aspect/LoggingAspect (AOP: @Before @After @Around)
                           │
                           ▼
          repository/  (@Repository, SQL + RowMapper)
                           │
                           ▼
                    JdbcTemplate  ──►  MySQL  bloggingPlatform
```
REST and GraphQL share the **same services and repositories**, so business rules exist in one place and there is only one data path.

| Package | Responsibility |
|---|---|
| `config` | `DatabaseConfig` (JdbcTemplate, transactions, startup connection check), `OpenApiConfig`, `AppProperties` |
| `controller` | REST endpoints: HTTP only, no business logic |
| `graphql` | GraphQL queries, mutations, field resolvers and the GraphQL error handler |
| `service` | Business rules and transactions |
| `repository` | SQL through JdbcTemplate (one class per table, plus reports) |
| `model` | Plain records matching the tables (not JPA entities), plus enums |
| `dto/request`, `dto/response` | What clients send (validated) and what the API returns |
| `exception` | Custom exceptions + `GlobalExceptionHandler` (`@RestControllerAdvice`) |
| `validation` | `@UniqueUsername`, `@UniqueEmail` and their validators |
| `aspect` | `LoggingAspect` and `PerformanceMonitor` |
| `algorithms` | Linear search, binary search, merge sort |
| `response` | `ApiResponse<T>`: the `{status, message, data}` wrapper |

## Project setup
Requirements: **JDK 21+**, **MySQL 8** with the `bloggingPlatform` database. Maven is not needed: use `./mvnw` (Windows: `mvnw.cmd`).

### Database configuration
The application uses the **existing** database and never changes its structure automatically. For a fresh install:
```bash
mysql -u root -p < src/main/resources/db/schema-mysql.sql      # tables, constraints, indexes
mysql -u root -p bloggingPlatform < src/main/resources/db/indexes.sql   # (only if the indexes are missing)
```
Credentials come from **environment variables**. The password is never stored in the code.
```powershell
$env:DB_PASSWORD = "your-mysql-password"
# optional: $env:DB_USERNAME = "root"; $env:DB_URL = "jdbc:mysql://localhost:3306/bloggingPlatform?serverTimezone=UTC"
```

**Existing database: run the soft-delete migration once** before starting this version. It adds a `deleted_at` column to the 5 main tables and keeps every existing row:
```bash
mysql -u root -p bloggingPlatform < src/main/resources/db/migration-soft-delete.sql
```

### Environment profiles
| Profile | Port | File | Database | Notes |
|---|---|---|---|---|
| `dev` (default) | **8080** | `application-dev.properties` | local MySQL | DEBUG logs incl. every SQL statement; Swagger and GraphiQL on |
| `test` | **8081** | `application-test.properties` | H2 in-memory, MySQL mode, a new database per test context | `schema.sql` + `data.sql` from `src/test/resources` |
| `prod` | **8082** | `application-prod.properties` | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | WARN logs; Swagger, API docs and GraphiQL off |

- Each profile has its **own port**, so several can run at the same time. `SERVER_PORT` overrides the port.
- **Swagger UI "Servers" drop-down:**
  - It lists all three profiles, from the `blog.servers[...]` properties in `application.properties`.
  - The profile that is running is listed first and marked **(active)**.
  - It documents where each profile runs; "Try it out" calls the active one.
  - Override a URL with `DEV_SERVER_URL`, `TEST_SERVER_URL` or `PROD_SERVER_URL`.
- **No CORS (not in the requirements):** the browser only lets Swagger's "Try it out" call the server that served the page. To test another profile, open that profile's own Swagger page (e.g. http://localhost:8081/swagger-ui/index.html). Postman and curl are not affected.

Shared settings are in `application.properties`, including `blog.monitoring.slow-threshold-ms=500` for the AOP "SLOW" warning.

## How to run
```bash
./mvnw spring-boot:run                                          # dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod          # another profile (prod runs on port 8082)
./mvnw clean package && java -jar target/blogPlatform-0.0.1-SNAPSHOT.jar
```
Expected log:
```
The following 1 profile is active: "dev"
Tomcat started on port 8080 (http) with context path '/'
Started BloggingApplication in 2.4 seconds
Connected to MySQL 8.0.46 at jdbc:mysql://localhost:3306/bloggingPlatform?serverTimezone=UTC
```

| URL | What |
|---|---|
| http://localhost:8080/swagger-ui/index.html | **Swagger UI**: try every REST endpoint |
| http://localhost:8080/v3/api-docs | OpenAPI document (JSON) |
| http://localhost:8080/graphiql | **GraphiQL** playground |
| http://localhost:8080/graphql | GraphQL endpoint (POST) |
| http://localhost:8080/api/system/info | Running profile and version |

## Spring IoC and dependency injection
- `@SpringBootApplication` scans `com.amali.blogPlatform`. Every `@RestController`, `@Controller`, `@Service`, `@Repository`, `@Component` and `@Configuration` becomes a **bean** that the IoC container creates and connects.
- **Constructor injection everywhere:** dependencies are `final` fields set in the constructor, and there is no field `@Autowired`. Example chain: `PostController(PostService)` → `PostService(PostRepository, UserRepository, TagRepository)` → `PostRepository(JdbcTemplate, NamedParameterJdbcTemplate)`.
- Unit tests create services with plain `new` and Mockito mocks, which constructor injection makes possible.

## REST API endpoints
Every response has the form `{"status": 200, "message": "...", "data": ...}`, errors included.

| Resource | Method & endpoint | Success | Errors |
|---|---|---|---|
| Users | `POST /api/users` | 201 + `Location` | 400, 409 duplicate username/email |
| | `GET /api/users`, `GET /api/users/{id}` | 200 | 404 |
| | `PUT /api/users/{id}` · `DELETE /api/users/{id}` | 200 · 204 | 400, 404, 409 |
| Auth | `POST /api/auth/login` (username **or** email) | 200 | 401 |
| Posts | `POST /api/posts` | 201 | 400, 404 unknown author/tag, 409 same title |
| | `GET /api/posts` (paged, sorted, filtered; see below) | 200 | 400 |
| | `GET /api/posts/search?keyword=` | 200 | 400 |
| | `GET /api/posts/{id}` · `PUT` · `DELETE` | 200 · 200 · 204 | 400, 404, 409 |
| Comments | `POST /api/posts/{postId}/comments` · `GET /api/posts/{postId}/comments` | 201 · 200 | 400, 404 |
| | `GET /api/comments?userId=&postAuthorId=` · `GET/PUT/DELETE /api/comments/{id}` | 200 / 204 | 400, 404 |
| Tags | `POST /api/tags` · `GET /api/tags` · `GET/PUT/DELETE /api/tags/{id}` | 201 / 200 / 204 | 400, 404, 409 |
| Reviews | `POST /api/posts/{postId}/reviews` · `GET /api/posts/{postId}/reviews` | 201 · 200 | 400 rating not 1-5, 404, 409 already reviewed |
| | `GET /api/reviews?userId=&postAuthorId=` · `GET/PUT/DELETE /api/reviews/{id}` | 200 / 204 | 400, 404 |
| Reports | `GET /api/reports/summary`, `/posts-per-tag`, `/top-rated?limit=`, `/most-commented?limit=` | 200 | 400 |
| Algorithms | `GET /api/algorithms/sort?sortBy=title\|quick&sortBy=title\|created_at\|rating` | 200 | 400 |
| | `GET /api/algorithms/search?algorithm=linear\|binary&title=` | 200 | 400 |
| Monitoring | `GET /api/metrics/methods` · `DELETE /api/metrics/methods` | 200 · 204 | – |
| System | `GET /api/system/info` | 200 | – |

### Paging, sorting, filtering, searching
```
GET /api/posts?page=0&size=5
GET /api/posts?sortBy=created_at&direction=DESC        sortBy: created_at | title | rating
GET /api/posts?authorId=3
GET /api/posts?tag=java&status=PUBLISHED&keyword=spring   (filters can be combined)
GET /api/posts/search?keyword=Java
```
`data` is a page: `{"content": [...], "page": 0, "size": 5, "totalElements": 17, "totalPages": 4, "first": true, "last": false}`. Paging is done in SQL (`LIMIT ? OFFSET ?`). The sort column comes from an allowed list, so `sortBy=password_hash` gives 400.

### Error examples
```json
{ "status": 400, "message": "Validation failed", "data": { "title": "Title is required" } }
{ "status": 409, "message": "Duplicate value",   "data": { "email": "Email 'alice@blog.com' is already registered" } }
{ "status": 404, "message": "User with id 999 not found", "data": null }
```

## GraphQL examples
Schema: `src/main/resources/graphql/schema.graphqls`. Try these in GraphiQL:
```graphql
query { posts { id title } }

query { post(id: "1") { title content author { username } comments { username content } tags { name } } }

query { postPage(page: 0, size: 5, sortBy: "title", direction: "ASC", tag: "java") {
          content { title averageRating } totalElements totalPages } }

query { users { username email } tags { name } }

mutation { createPost(title: "Spring Boot", content: "Learning GraphQL with Spring Boot today",
                      authorId: "1", status: PUBLISHED, tagIds: ["2"]) { id title } }

mutation { updatePost(id: "18", title: "Spring Boot GraphQL", content: "Updated content that is long enough") { id title } }

mutation { deletePost(id: "18") }
```
Errors come back in `errors[]` with a `classification` (`NOT_FOUND`, `BAD_REQUEST`, …). Validation errors also list `extensions.fieldErrors`. `Post.author` uses `@BatchMapping`, so the authors of a whole list are loaded with a single query.

## Validation and exception handling
- **Request DTOs** carry Bean Validation rules: `@NotBlank`, `@Size`, `@Email`, `@Pattern`, `@NotNull`, `@Positive`, `@Min`/`@Max` (rating 1-5). Controllers use `@Valid`. Query parameters are checked too (`size` must be 1-100).
- **Custom validators** `@UniqueUsername`, `@UniqueEmail` and `@UniquePostTitle` query the database through the repositories. When they fail, the answer is **409**.
- **Post titles are unique across ALL posts**, whoever wrote them, and upper/lower case does not count ("Java Tips" = "java tips"). `@UniquePostTitle` checks new posts; `PostService` also checks updates when the title changes.
- **Custom exceptions:** `UserNotFoundException`, `PostNotFoundException`, `CommentNotFoundException`, `TagNotFoundException`, `ReviewNotFoundException` (404); `DuplicateUsernameException`, `DuplicateEmailException`, `DuplicateResourceException` (409); `InvalidParameterException` (400); `InvalidCredentialsException` (401).
- **`GlobalExceptionHandler`** (`@RestControllerAdvice`) turns every exception into the `ApiResponse` shape:
  - 400: validation, bad JSON, wrong type, missing parameter, database constraint
  - 401, 404, 405, 409
  - 500: a generic message only; the details go to the log.

## Soft delete
`DELETE` requests do **not** remove rows. They set `deleted_at` to the current time (`UPDATE ... SET deleted_at = CURRENT_TIMESTAMP`), and every query reads only rows `WHERE deleted_at IS NULL`.

| Deleted | What happens (one transaction) |
|---|---|
| Post | The post, its comments and its reviews are marked deleted. |
| User | The user, their posts, the comments and reviews on those posts, and their own comments and reviews are marked deleted. The user can no longer log in. |
| Tag | The tag is marked deleted and disappears from every post and from `?tag=` filters. |
| Comment / review | Only that row is marked deleted. |

- A deleted record answers **404**, like one that never existed. Deleting it again also gives 404.
- **Unique values stay reserved.** The deleted row still exists, so a deleted user's username and email, and a deleted post's title, cannot be reused (409).
- **Two cases restore the old row instead of creating a new one:**
  - Creating a tag with a deleted tag's name brings that tag back (same id).
  - Reviewing a post again after deleting your review brings back that review with the new rating.
- Why soft delete: data can be recovered, history and reports stay correct, and nothing is lost by mistake.

## AOP implementation
`aspect/LoggingAspect` applies to every method in `com.amali.blogPlatform.service` (pointcut `execution(* com.amali.blogPlatform.service.*.*(..))`):

| Advice | What it does |
|---|---|
| `@Before` | `Executing: PostService.createPost with [...]`, with passwords masked as `***` |
| `@After` | `Finished: PostService.createPost` (also after errors) |
| `@AfterThrowing` | `PostService.getPostById failed: PostNotFoundException - ...` (logs only, never swallows) |
| `@Around` | measures time with `System.nanoTime()`: `PostService.getPosts took 24.12 ms`, or WARN `SLOW: ...` above `blog.monitoring.slow-threshold-ms`. It also records the call in `PerformanceMonitor`. |

`GET /api/metrics/methods` shows calls, errors, and total, average and max milliseconds for every service method.

## Testing
```bash
./mvnw test          # 103 automated tests (H2, no MySQL needed)
```
| Kind | Classes |
|---|---|
| Unit (Mockito) | `UserServiceTest`, `PostServiceTest`, `AlgorithmsTest` |
| Repository (`@JdbcTest`, real SQL on H2) | `PostRepositoryTest`, `UserRepositoryTest`, `CommentTagReviewRepositoryTest` |
| Web layer (`@WebMvcTest`) | `SystemControllerTest` |
| Integration (`@SpringBootTest` + MockMvc) | `ReadEndpointsIntegrationTest`, `CrudEndpointsIntegrationTest`, `PostQueryIntegrationTest`, `ValidationIntegrationTest`, `ReportAndListEndpointsTest`, `OpenApiDocumentationTest` |
| GraphQL | `GraphQlIntegrationTest` (the 7 acceptance cases plus errors, and HTTP `POST /graphql`) |
| AOP | `LoggingAspectTest` (log lines, timing, metrics, errors not swallowed, password masking) |
| Performance (manual) | `QueryBenchmarkTest`: click Run in IntelliJ, or `./mvnw test -Pbenchmark` (1,000 posts by default, `-DbenchmarkPosts=100000` for more); prints a report in the console. Also `docs/performance/rest-vs-graphql.sh` |

**Postman:** import `docs/postman/blog-platform.postman_collection.json` and run the whole collection (Collection Runner). Its 87 requests in 13 folders cover every REST endpoint (CRUD, paging, sorting, filtering, search, reports, algorithms, metrics) and GraphQL queries and mutations. They create a user, tag, post, comment and review with a unique run number, check every status code (201/200/204/400/401/404/409), and then delete everything they created, so it can be run again and again. `baseUrl` is `http://localhost:8080` (dev); change it to `http://localhost:8081` to test the test profile.

## Performance findings
These were measured on 100,000 posts; full details are in [`docs/performance-report.md`](docs/performance-report.md).
- **Indexes:**
  - First page newest first: 286 ms → **2.7 ms** (107×).
  - Sort by title: 291 ms → **2.2 ms** (135×).
  - A FULLTEXT index answers a rare-word search in **0.9 ms**, against about 430 ms for `LIKE '%…%'`.
- **REST vs GraphQL:**
  - The same speed for one resource.
  - GraphQL sends **92 % less data** when only some fields are needed.
  - GraphQL is **3.8× faster** when a screen combines resources: 1 request instead of 11 for 10 posts with their authors.
- **Still slow:** sorting by the computed average rating (about 1.7 s) and deep `OFFSET` pages (about 0.4 s at page 5,000). The report recommends a stored rating column and keyset pagination.
