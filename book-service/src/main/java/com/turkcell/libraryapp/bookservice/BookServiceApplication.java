package com.turkcell.libraryapp.bookservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

// Auditing burada AÇILMAZ: JpaConfig'te (@EnableJpaAuditing(auditorAwareRef = "auditorProvider"))
// zaten açık. İkisi birlikte jpaAuditingHandler bean'ini iki kez tanımlar.
@SpringBootApplication
@EnableCaching
public class BookServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookServiceApplication.class, args);
    }
}







