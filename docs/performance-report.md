# Performance Report – Blogging Platform API

## 1. Introduction
This report measures how fast the Blogging Platform API answers. It compares:

1. REST and GraphQL,
2. database searching with and without indexes,
3. pagination at different page numbers,
4. sorting by the different columns,
5. the execution time of each database query.

Every number below was **measured on this project**, not estimated. Section 2 explains how to measure them again.

## 2. Testing Environment
| Item | Value |
|---|---|
| Machine | Windows 11 Pro, local laptop (API, database and test client on the same machine) |
| Java | JDK 27 runtime, code compiled for Java 21 |
| Framework | Spring Boot 3.5.7, Spring JDBC (`JdbcTemplate`), HikariCP connection pool |
| Database | MySQL 8.0.46 (InnoDB) |
| Data set | Separate database `bloggingPlatform_perf`, built by `src/test/resources/benchmark/bulk_data.sql`: **1,000 users, 30 tags, 100,000 posts, 200,000 tag links, 200,000 comments, 100,000 reviews**. The real `bloggingPlatform` database (12 users, 17 posts) is too small to show differences, and it was never changed. |
| SQL timing | `QueryBenchmarkTest`: calls the real `PostRepository` methods, 3 warm-up runs, then the **median of 15 runs** |
| HTTP timing | `docs/performance/rest-vs-graphql.sh`: `curl` `time_total` (connect, server work and download), 5 warm-up requests, then the **median of 30 requests** |
| Logging during HTTP tests | Application log level WARN, so writing logs does not distort the times. The AOP monitor still records every service call. |

How to measure again:
```bash
$env:DB_PASSWORD = "..."
./mvnw test -Pbenchmark -DbenchmarkPosts=100000          # SQL, without vs with indexes
# start the API on the benchmark database, then:
bash docs/performance/rest-vs-graphql.sh 30                       # HTTP, REST vs GraphQL
```

## 3. REST API Performance
These are REST requests at the HTTP level, on 100,000 posts, with indexes in place.

| Request | Median ms | Response bytes |
|---|---:|---:|
| `GET /api/posts?page=0&size=10` (newest first) | 24.0 | 6,122 |
| `GET /api/posts?size=100` | 29.1 | 59,692 |
| `GET /api/posts?authorId=7` | 5.5 | 6,097 |
| `GET /api/posts?sortBy=title&direction=ASC` | 23.8 | 6,065 |
| `GET /api/posts/1` | 8.4 | 640 |
| `GET /api/posts?tag=tag-5` | 137.6 | 6,089 |
| `GET /api/posts?sortBy=rating` | 1,739.2 | 6,131 |

The SQL for page 0 takes only about **2.7 ms** (section 5). Most of the 24 ms is the `COUNT(*)` that computes `totalElements` (about 20 ms on 100,000 rows), plus turning the rows into JSON and the HTTP round trip. `authorId=7` is fast because the count only covers that author's 1,000 posts.

## 4. GraphQL Performance
These are the same data needs, requested once through REST and once through GraphQL.

| Scenario | REST requests | REST ms | REST bytes | GraphQL requests | GraphQL ms | GraphQL bytes |
|---|---:|---:|---:|---:|---:|---:|
| One post, all fields | 1 | 8.4 | 640 | 1 | 7.6 | 604 |
| 10 newest posts, all fields | 1 | 27.3 | 6,122 | 1 | 27.0 | 6,059 |
| 10 newest posts, **only id + title** | 1 | 24.5 | 6,122 | 1 | 25.7 | **480** |
| Post + author + comments | **3** | 13.2 | 1,214 | **1** | **9.8** | **255** |
| 10 posts + their authors | **11** | 102.9 | 7,795 | **1** | **27.3** | **659** |

## 5. Database Query Optimization
These are the SQL queries the API runs (through the real repository code), on 100,000 posts.

| Query | Without indexes (ms) | With indexes (ms) | Faster |
|---|---:|---:|---:|
| Page 0, newest first (`ORDER BY created_at DESC LIMIT 10`) | 286.39 | 2.67 | **107×** |
| Published posts, newest first | 267.31 | 2.38 | **112×** |
| One author's posts | 8.66 | 2.33 | 3.7× |
| Sort by title | 291.30 | 2.16 | **135×** |
| Filter by tag | 132.16 | 129.51 | 1.0× |
| Sort by average rating | 1,671.39 | 1,728.31 | 1.0× |
| Deep page (`OFFSET 50000`) | 463.16 | 420.97 | 1.1× |
| `LIKE '%topic 7%'` (common words) | 491.71 | 7.27 | 67.6× |
| `LIKE '%77777%'` (rare word) | 427.37 | 661.24 | 0.6× |
| `COUNT(*)` for `totalElements` | 25.13 | 20.53 | 1.2× |
| `MATCH(title, content) AGAINST ('77777')` with a FULLTEXT index | – | **0.94** | – |

Indexes used (`src/main/resources/db/indexes.sql`):
- `idx_posts_created_at (created_at)`, **added in Phase 4** for the default listing;
- `idx_posts_status_created (status, created_at)`;
- `idx_posts_author_created (author_id, created_at)`;
- `idx_posts_title (title)`.

Every primary key, unique constraint and foreign key column (`comments.post_id`, `reviews.post_id`, `post_tags.tag_id`, …) already has its own InnoDB index.

**Why some queries got faster and others did not:**
- **`ORDER BY … LIMIT 10` with an index on the sort column (107–135× faster).** MySQL reads the first 10 entries of the index in order and stops. Without the index it must sort all 100,000 rows first.
- **The common-word `LIKE` search (68× faster).** The B-tree index cannot search inside text. The speed-up comes from the `created_at` index instead: MySQL walks the posts newest first and stops once it has 10 matches, which happens quickly because many posts match.
- **The rare-word `LIKE` search (no gain, even slower).** `LIKE '%…%'` must look at almost every row whatever the index. Walking the index in date order adds random reads. A **FULLTEXT index answers the same question in 0.94 ms, about 450× faster**.
- **Sort by rating (no gain).** The average rating is computed by a subquery for every post, and a computed value cannot be indexed.
- **Tag filter (no gain).** The subquery on `post_tags` already uses the foreign key index. The cost is joining 6,667 matching posts and sorting them.
- **Deep page (no gain).** `OFFSET 50000` still has to read and skip 50,000 index entries.

## 6. Searching and Sorting Analysis
### 6.1 In the database (what the API uses)
Pagination uses `LIMIT ? OFFSET ?`, so only one page leaves the database. Sorting uses `ORDER BY`, with the column taken from an **allowed list** (`PostSortField`), never from the user's text. Search uses `LIKE ?` with `%keyword%` as a parameter.

Pagination cost depends on the page number:

| Page (size 10) | Median ms (HTTP) |
|---|---:|
| `page=0` | 24.0 |
| `page=100` (OFFSET 1,000) | 29.4 |
| `page=5000` (OFFSET 50,000) | 435.5 |

OFFSET pagination is cheap for the first pages, which is where readers usually stay. Its cost grows with the page number.

### 6.2 In Java (algorithm demos, `algorithms/` package)
| Algorithm | Time | Measured |
|---|---|---|
| Linear search | O(n) | On the real 17 posts, finding "JAVA" took **1** comparison (it was first); a missing title takes 17. In the unit test with 1,000 sorted titles, `title0777` took **778** comparisons. |
| Binary search | O(log n) | Same 1,000 titles: **10 or fewer** comparisons. On the 17 real posts: **5**. The list must be sorted first. |
| Merge sort | O(n log n) always, stable | Correct on 5,000 random words (unit test), and keeps the order of equal items. Quick sort was removed: merge sort is enough for the demo, never hits an O(n²) worst case, and is stable. |

The Java algorithms show the theory. Real API requests use SQL, which needs no loading of all rows into memory and can use indexes. This follows the brief: "use database-native techniques for scalable API operations".

## 7. Performance Comparison
- **REST vs GraphQL, one resource:** about the same (8.4 vs 7.6 ms, 27.3 vs 27.0 ms). Both call the same service, repository and SQL, so neither is faster on its own.
- **GraphQL returns only what is asked for:** for "id + title of 10 posts", GraphQL sent **480 bytes against REST's 6,122 (−92 %)**. REST always sends the whole post.
- **GraphQL avoids extra round trips:**
  - "Post + author + comments" needs 3 REST calls (13.2 ms, 1,214 bytes) but 1 GraphQL call (9.8 ms, 255 bytes).
  - "10 posts with their authors" needs **11** REST calls (**102.9 ms**) but **1** GraphQL call (**27.3 ms, 3.8× faster**).
  - Inside that GraphQL call, `@BatchMapping` loads all 10 authors with **one** `WHERE user_id IN (…)` query. The AOP monitor counted 72 `UserService.getUsersByIds` calls for 72 GraphQL requests, so exactly one per request and never one per post (no N+1).
- **Indexes matter more than the API style:** the right index turns a 290 ms query into 2–3 ms, a far bigger effect than REST vs GraphQL.

AOP measurements (`GET /api/metrics/methods`) during the HTTP benchmark:

| Service method | Calls | Average ms | Max ms |
|---|---:|---:|---:|
| `PostService.getPosts` | 504 | 180.04 | 2,010.00 |
| `PostService.searchPosts` | 72 | 613.85 | 1,635.39 |
| `PostService.getPostById` | 144 | 2.27 | 29.10 |
| `CommentService.getCommentsByPost` | 72 | 1.30 | 6.00 |
| `UserService.getUserById` | 396 | 0.63 | 1.58 |
| `UserService.getUsersByIds` | 72 | 0.64 | 1.10 |

The averages of `getPosts` and `searchPosts` are pulled up by the rating sort, the deep page and the rare-word search. The aspect logs these as `SLOW: … took … ms` because they pass the 500 ms threshold. That is exactly how the monitor is meant to point at slow operations.

## 8. Challenges
- **Pagination needs two queries** (the page and `COUNT(*)`). On large tables the count costs more than the page itself.
- **Sort column names cannot be `?` parameters.** They had to come from an allowed list, to prevent SQL injection.
- **`LIKE '%word%'` cannot use a normal index.** FULLTEXT would fix that, but it finds whole words only and the H2 test database does not support it.
- **The average rating is computed for every row**, so "sort by rating" reads all posts.
- **Two databases:** tests run on H2 in MySQL mode. Some MySQL features (FULLTEXT, `ON UPDATE` columns) had to be avoided or replaced. For example, `updated_at` is set explicitly in the UPDATE statement.
- **Generated keys:** H2 returns every default-valued column as a "generated key", so the key column is named explicitly (`new String[]{"post_id"}`).
- **Fair measurements:** warm-up runs, medians instead of averages, one reused connection for SQL timings, and reduced logging for HTTP timings.

## 9. Recommendations
1. **Add a FULLTEXT index and use `MATCH … AGAINST` in MySQL** for keyword search. It measured about 450× faster than `LIKE` for a rare word. Keep `LIKE` as the fallback for the H2 tests.
2. **Store the average rating and comment count in `posts`**, kept up to date when a review or comment changes. "Sort by rating" can then use an index instead of a subquery per row (1.7 s → a few ms).
3. **Keyset ("seek") pagination** for deep pages: `WHERE created_at < ? ORDER BY created_at DESC LIMIT 10` instead of `OFFSET`. It costs the same on page 5,000 as on page 0.
4. **Cache or skip the `COUNT(*)`** on the first page of very large lists, or show "more" instead of an exact total.
5. **Use GraphQL for screens that combine data** (a post with its author and comments) or need few fields. Use REST for simple CRUD and for clients that need HTTP caching.
6. **Keep the AOP monitor on in production with a sensible threshold**, so slow service methods show up in the logs early.

## 10. Conclusion
The API meets the performance goals for normal use. With the indexes, the usual requests (a page of posts, one post, an author's posts, sorting by date or title) answer in **2–3 ms of SQL and about 5–30 ms over HTTP on 100,000 posts**.

Indexes gave the largest improvement (**up to 135×**). GraphQL gave the second: for screens that combine resources it needed **up to 11× fewer requests and 3.8× less time**, and it cut payloads by **up to 92 %**. Two operations stay slow: sorting by the computed rating, and substring search on a rare word. Section 9 gives a concrete fix for each.
