package com.turkcell.libraryapp.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

// Eureka kaydı spring-cloud-starter-netflix-eureka-client ile otomatik yapılandırılır;
// @EnableEurekaClient Spring Cloud 2025.x'te kaldırıldı.
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}



