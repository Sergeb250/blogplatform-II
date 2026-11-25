package com.amali.blogPlatform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Database access configuration. Spring Boot creates the {@link DataSource} (a HikariCP connection
 * pool) from the {@code spring.datasource.*} properties of the active profile; this class builds the
 * JDBC helpers on top of it:
 * <ul>
 *   <li>{@link JdbcTemplate}: runs SQL with {@code ?} placeholders, opens and closes connections,
 *       and turns {@link SQLException}s into Spring's {@code DataAccessException}s;</li>
 *   <li>{@link NamedParameterJdbcTemplate}: the same with {@code :name} placeholders, used for
 *       {@code IN (:ids)} lists.</li>
 * </ul>
 * {@code @EnableTransactionManagement} makes {@code @Transactional} service methods run on one
 * connection and commit or roll back together (the transaction manager is Boot's
 * {@code DataSourceTransactionManager}).
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource,
                                     @Value("${blog.jdbc.query-timeout-seconds:10}") int queryTimeoutSeconds) {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setQueryTimeout(queryTimeoutSeconds);
        return jdbcTemplate;
    }

    @Bean
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate(JdbcTemplate jdbcTemplate) {
        return new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    /**
     * Checks the database connection once at startup and logs which database answered. A failure is
     * logged, not thrown, so the API (and Swagger) still start while the database is down.
     */
    @Bean
    public ApplicationRunner databaseConnectionCheck(DataSource dataSource) {
        return args -> {
            try (Connection connection = dataSource.getConnection()) {
                DatabaseMetaData meta = connection.getMetaData();
                log.info("Connected to {} {} at {}", meta.getDatabaseProductName(),
                        meta.getDatabaseProductVersion(), meta.getURL());
            } catch (SQLException e) {
                log.error("Could not connect to the database: {}", e.getMessage());
            }
        };
    }
}
