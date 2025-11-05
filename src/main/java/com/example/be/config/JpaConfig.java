package com.example.be.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = {
        "com.example.be.repository",
        "com.example.be.notification.repository" // ← thêm package này
})
public class JpaConfig {
}
