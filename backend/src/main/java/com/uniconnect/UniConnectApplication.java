package com.uniconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@EntityScan(basePackages = "com.uniconnect.models")
@EnableJpaRepositories(basePackages = "com.uniconnect.repositories")
public class UniConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniConnectApplication.class, args);
    }
}
