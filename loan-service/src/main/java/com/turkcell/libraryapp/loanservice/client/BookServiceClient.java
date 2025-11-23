package com.turkcell.libraryapp.loanservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "book-service", path = "/api/copybooks")
public interface BookServiceClient {
    
    @GetMapping("/{id}")
    ResponseEntity<Map<String, Object>> getCopyBookById(@PathVariable Long id);
    
    @GetMapping("/{id}/exists")
    Boolean existsById(@PathVariable Long id);
    
    @GetMapping("/{id}/available")
    Boolean isAvailable(@PathVariable Long id);
}



