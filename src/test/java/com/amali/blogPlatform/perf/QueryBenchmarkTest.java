package com.amali.blogPlatform.perf;

import com.amali.blogPlatform.model.PostFilter;
import com.amali.blogPlatform.model.PostSortField;
import com.amali.blogPlatform.model.PostStatus;
import com.amali.blogPlatform.repository.PostRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Measures the SQL of the post queries (paging, sorting, filtering, search) on a SEPARATE MySQL database
 * filled with generated data, first WITHOUT and then WITH the indexes of {@code db/indexes.sql}, and prints
 * a report in the console. The real database is only read (to copy its table definitions).
 *
 * <h2>How to run</h2>
 * <ul>
 *   <li><b>IntelliJ:</b> open this class and click the green Run arrow. The report appears in the Run window.
 *       (DB_PASSWORD must be set as an environment variable; restart IntelliJ after {@code setx}.)</li>
 *   <li><b>Command line:</b> {@code ./mvnw test -Pbenchmark} (the "benchmark" Maven profile in the pom)</li>
 *   <li><b>Another size:</b> add {@code -DbenchmarkPosts=100000} (default: 1,000 posts).</li>
 * </ul>
 * The {@code @Tag("benchmark")} keeps it out of the normal {@code ./mvnw test} run (see the pom), because it
 * needs MySQL and takes longer than the other tests.
 *
 * <h2>Steps</h2>
 * Build {@code bloggingPlatform_perf} with {@code benchmark/bulk_data.sql}, drop the post indexes, time every
 * query (3 warm-up runs, then the median of 15 runs) through the REAL {@link PostRepository}, create the
 * indexes, time again, print the report and save it to {@code target/benchmark/query-benchmark.md}.
 */
@Tag("benchmark")
class QueryBenchmarkTest {

    /** Number of posts to generate; change it with -DbenchmarkPosts=... */
    private static final int POSTS = Integer.getInteger("benchmarkPosts", 1_000);
    private static final int COMMENTS = POSTS * 2;
    private static final int WARM_UP = 3;
    private static final int RUNS = 15;
    private static final List<String> POST_INDEXES = List.of(
            "idx_posts_status_created", "idx_posts_author_created", "idx_posts_title", "idx_posts_created_at");

    private final JdbcTemplate serverJdbc = new JdbcTemplate(dataSource(""));
    private final JdbcTemplate perfJdbc = new JdbcTemplate(dataSource("bloggingPlatform_perf"));
    private final PostRepository posts = new PostRepository(perfJdbc, new NamedParameterJdbcTemplate(perfJdbc));

    @Test
    void postQueriesWithoutAndWithIndexes() throws IOException {
        checkPasswordIsSet();
        System.out.printf("%nBuilding bloggingPlatform_perf with %,d posts ...%n", POSTS);
        long buildStart = System.nanoTime();
        runScript(Path.of("src", "test", "resources", "benchmark", "bulk_data.sql"));
        double buildSeconds = (System.nanoTime() - buildStart) / 1_000_000_000.0;

        System.out.println("Measuring WITHOUT indexes ...");
        for (String index : POST_INDEXES) {
            dropIndexIfExists(index);
        }
        perfJdbc.execute("ANALYZE TABLE posts");
        Map<String, Double> without = measureAll();

        System.out.println("Measuring WITH indexes ...");
        for (String sql : readStatements(Path.of("src", "main", "resources", "db", "indexes.sql"))) {
            perfJdbc.execute(sql);
        }
        perfJdbc.execute("CREATE FULLTEXT INDEX ft_posts_title_content ON posts (title, content)");
        perfJdbc.execute("ANALYZE TABLE posts");
        Map<String, Double> with = measureAll();
        with.put("Search rare word: FULLTEXT MATCH ... AGAINST", median(() -> perfJdbc.queryForList(
                "SELECT post_id FROM posts WHERE MATCH(title, content) AGAINST (?) ORDER BY created_at DESC LIMIT 10",
                "zzrareword")));

        printAndSaveReport(without, with, buildSeconds);
    }

    /** The queries the API runs, each through the real repository code. */
    private Map<String, Double> measureAll() {
        Map<String, Double> results = new LinkedHashMap<>();
        PostFilter none = PostFilter.none();
        int deepOffset = (POSTS / 2) / 10 * 10;           // a page in the middle of the data
        results.put("GET /api/posts (page 0, newest first)",
                median(() -> posts.findPage(none, PostSortField.CREATED_AT, false, 10, 0)));
        results.put("GET /api/posts?status=PUBLISHED",
                median(() -> posts.findPage(new PostFilter(null, null, PostStatus.PUBLISHED, null),
                        PostSortField.CREATED_AT, false, 10, 0)));
        results.put("GET /api/posts?authorId=7",
                median(() -> posts.findPage(new PostFilter(7L, null, null, null), PostSortField.CREATED_AT, false, 10, 0)));
        results.put("GET /api/posts?sortBy=title&direction=ASC",
                median(() -> posts.findPage(none, PostSortField.TITLE, true, 10, 0)));
        results.put("GET /api/posts?tag=tag-5",
                median(() -> posts.findPage(new PostFilter(null, "tag-5", null, null), PostSortField.CREATED_AT, false, 10, 0)));
        results.put("GET /api/posts?sortBy=rating",
                median(() -> posts.findPage(none, PostSortField.RATING, false, 10, 0)));
        results.put("GET /api/posts deep page (OFFSET " + deepOffset + ")",
                median(() -> posts.findPage(none, PostSortField.CREATED_AT, false, 10, deepOffset)));
        results.put("Search 'topic 7' (LIKE, common words)",
                median(() -> posts.findPage(new PostFilter(null, null, null, "topic 7"), PostSortField.CREATED_AT, false, 10, 0)));
        results.put("Search rare word (LIKE)",
                median(() -> posts.findPage(new PostFilter(null, null, null, "zzrareword"), PostSortField.CREATED_AT, false, 10, 0)));
        results.put("COUNT for totalElements (no filter)",
                median(() -> posts.countPosts(none)));
        return results;
    }

    /** Median time in milliseconds of {@link #RUNS} runs, after {@link #WARM_UP} unmeasured runs. */
    private static double median(Runnable query) {
        for (int i = 0; i < WARM_UP; i++) {
            query.run();                      // warm-up: fills MySQL's buffer pool and the JDBC caches
        }
        List<Double> times = new ArrayList<>();
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            query.run();
            times.add((System.nanoTime() - start) / 1_000_000.0);
        }
        times.sort(null);
        return times.get(times.size() / 2);
    }

    // ------------------------------------------------------------------ report

    private void printAndSaveReport(Map<String, Double> without, Map<String, Double> with, double buildSeconds)
            throws IOException {
        String line = "=".repeat(96);
        String thin = "-".repeat(96);
        StringBuilder report = new StringBuilder();
        report.append('\n').append(line).append('\n');
        report.append("  QUERY BENCHMARK REPORT\n");
        report.append(line).append('\n');
        report.append(String.format("  Date          : %s%n",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
        report.append(String.format("  Database      : bloggingPlatform_perf (MySQL %s), separate from the real data%n",
                serverJdbc.queryForObject("SELECT VERSION()", String.class)));
        report.append(String.format("  Data          : %,d posts, %,d comments, %,d reviews, %,d tag links, 1,000 users, 30 tags%n",
                POSTS, COMMENTS, POSTS, POSTS * 2));
        report.append(String.format("  Data built in : %.1f s%n", buildSeconds));
        report.append(String.format("  Method        : %d warm-up runs, then the MEDIAN of %d runs per query (milliseconds)%n",
                WARM_UP, RUNS));
        report.append(thin).append('\n');
        report.append(String.format("  %-50s %13s %13s %12s%n", "Query", "No index", "With index", "Speed-up"));
        report.append(thin).append('\n');

        double totalWithout = 0;
        double totalWith = 0;
        String biggestWin = null;
        double biggestSpeedUp = 0;
        for (String query : with.keySet()) {
            Double before = without.get(query);
            double after = with.get(query);
            String speedUp = "-";
            if (before != null) {
                double factor = before / after;
                speedUp = String.format("%.1fx", factor);
                totalWithout += before;
                totalWith += after;
                if (factor > biggestSpeedUp) {
                    biggestSpeedUp = factor;
                    biggestWin = query;
                }
            }
            report.append(String.format("  %-50s %13s %13.2f %12s%n", shorten(query),
                    before == null ? "-" : String.format("%.2f", before), after, speedUp));
        }
        report.append(thin).append('\n');
        report.append(String.format("  %-50s %13.2f %13.2f %12s%n", "TOTAL (the 10 API queries)",
                totalWithout, totalWith, String.format("%.1fx", totalWithout / totalWith)));
        report.append(line).append('\n');
        report.append("  Summary\n");
        report.append(String.format("  - Biggest gain: %s (%.1fx faster with an index).%n", biggestWin, biggestSpeedUp));
        report.append("  - Sort by rating stays slow: the average is computed for every post, so no index can help.\n");
        report.append("  - LIKE '%word%' cannot use a normal index; FULLTEXT MATCH ... AGAINST can.\n");
        if (POSTS < 10_000) {
            report.append(String.format("  - With only %,d posts every query is fast; run with -DbenchmarkPosts=100000%n", POSTS));
            report.append("    to see how the indexes matter when the table grows.\n");
        }
        report.append(line).append('\n');

        System.out.println(report);

        Path out = Path.of("target", "benchmark", "query-benchmark.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, "```\n" + report + "```\n");
        System.out.println("  Report saved to " + out.toAbsolutePath() + "\n");
    }

    private static String shorten(String text) {
        return text.length() <= 50 ? text : text.substring(0, 47) + "...";
    }

    // ------------------------------------------------------------------ helpers

    /** Stops with a clear message instead of a long "Access denied ... (using password: NO)" stack trace. */
    private static void checkPasswordIsSet() {
        if (dbPassword().isEmpty()) {
            throw new IllegalStateException("""

                    No MySQL password found. Save it once (single quotes keep the $ sign):
                        setx DB_PASSWORD 'your-mysql-password'
                    or set it only for this PowerShell window:
                        $env:DB_PASSWORD = 'your-mysql-password'
                    Then run again: ./mvnw test -Pbenchmark
                    """);
        }
    }

    /**
     * The MySQL password: the DB_PASSWORD environment variable of this terminal, or else (on Windows) the
     * DB_PASSWORD saved for the user with {@code setx}. {@code setx} only reaches terminals opened AFTER it,
     * so an older terminal or IntelliJ does not see it; reading the saved value directly from the Windows
     * user settings (registry key HKCU\Environment, where setx writes it) makes it work in any terminal.
     * The password is never written in the code or printed.
     */
    private static String dbPassword() {
        String fromTerminal = System.getenv("DB_PASSWORD");
        if (fromTerminal != null && !fromTerminal.isEmpty()) {
            return fromTerminal;
        }
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            return "";
        }
        try {
            Process process = new ProcessBuilder("reg", "query", "HKCU\\Environment", "/v", "DB_PASSWORD")
                    .redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes());
            process.waitFor();
            for (String line : output.split("\\R")) {
                // line looks like:    DB_PASSWORD    REG_SZ    the-password
                String[] parts = line.trim().split("\\s+REG_(?:EXPAND_)?SZ\\s+", 2);
                if (parts.length == 2 && parts[0].equals("DB_PASSWORD")) {
                    return parts[1];
                }
            }
        } catch (IOException e) {
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "";
    }

    private void dropIndexIfExists(String index) {
        Long count = perfJdbc.queryForObject("SELECT COUNT(*) FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'posts' AND INDEX_NAME = ?", Long.class, index);
        if (count != null && count > 0) {
            perfJdbc.execute("DROP INDEX " + index + " ON posts");
        }
    }

    /** Runs the bulk script after replacing the size placeholders ${POSTS} and ${COMMENTS}. */
    private void runScript(Path file) throws IOException {
        for (String sql : readStatements(file)) {
            serverJdbc.execute(sql
                    .replace("${POSTS}", String.valueOf(POSTS))
                    .replace("${COMMENTS}", String.valueOf(COMMENTS)));
        }
    }

    /** The statements of a .sql file: split on ';', lines starting with -- are comments. */
    private static List<String> readStatements(Path file) throws IOException {
        StringBuilder text = new StringBuilder();
        for (String line : Files.readAllLines(file)) {
            if (!line.strip().startsWith("--")) {
                text.append(line).append('\n');
            }
        }
        List<String> statements = new ArrayList<>();
        for (String sql : text.toString().split(";")) {
            if (!sql.isBlank()) {
                statements.add(sql.strip());
            }
        }
        return statements;
    }

    /** One connection that is reused for every query, so opening connections is not part of the timings. */
    private static SingleConnectionDataSource dataSource(String database) {
        String host = System.getenv().getOrDefault("DB_HOST", "localhost:3306");
        SingleConnectionDataSource ds = new SingleConnectionDataSource(
                "jdbc:mysql://" + host + "/" + database + "?serverTimezone=UTC",
                System.getenv().getOrDefault("DB_USERNAME", "root"),
                dbPassword(), true);
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return ds;
    }
}
