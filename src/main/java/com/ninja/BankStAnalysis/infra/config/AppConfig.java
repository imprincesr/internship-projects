package com.ninja.BankStAnalysis.infra.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.unit.DataSize;

import javax.sql.DataSource;

@Configuration
public class AppConfig {

    //JSON Configuration (Supports LocalDateTime)
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Enables LocalDateTime support
        return objectMapper;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource); // Spring provides DataSource
    }

    //File Upload Configuration (Allows Large File Uploads)
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofGigabytes(5)); // Set max file size to 5GB
        factory.setMaxRequestSize(DataSize.ofGigabytes(10)); // Set max request size to 10GB
        return factory.createMultipartConfig();
    }
}