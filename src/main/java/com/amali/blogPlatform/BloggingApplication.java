package com.amali.blogPlatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point of the Blogging Platform backend.
 * <p>
 * {@code @SpringBootApplication} turns on component scanning for this package and its sub-packages
 * ({@code controller}, {@code service}, {@code repository}, ...), so every class annotated with
 * {@code @RestController}, {@code @Service}, {@code @Repository}, {@code @Component} or
 * {@code @Configuration} becomes a bean in the Spring IoC container. The container creates the beans
 * and passes their dependencies through their constructors (constructor injection).
 * {@code @ConfigurationPropertiesScan} registers the typed {@code blog.*} settings in {@code config}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BloggingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BloggingApplication.class, args);
    }
}
