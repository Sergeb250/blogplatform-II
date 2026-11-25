package com.amali.blogPlatform;

import com.amali.blogPlatform.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BloggingApplicationTests {

    @Autowired
    private AppProperties properties;

    @Test
    void contextLoads() {
        assertThat(properties.name()).isEqualTo("Blogging Platform API");
    }

    @Test
    void testProfileSettingsAreUsed() {
        assertThat(properties.environment()).isEqualTo("test");
    }
}
