package com.turkcell.libraryapp.reservationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "book-service", path = "/api/copybooks")
public interface BookServiceClient {
    
    @GetMapping("/{id}")
    ResponseEntity<Map<String, Object>> getCopyBookById(@PathVariable Long id);
}



