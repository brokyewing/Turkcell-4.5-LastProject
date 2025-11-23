package com.turkcell.libraryapp.fineservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "loan-service", path = "/api/loans")
public interface LoanServiceClient {
    
    @GetMapping("/{id}")
    ResponseEntity<Map<String, Object>> getLoanById(@PathVariable Long id);
}



