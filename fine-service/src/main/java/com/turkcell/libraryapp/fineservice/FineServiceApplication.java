package com.turkcell.libraryapp.fineservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class FineServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FineServiceApplication.class, args);
    }
}







