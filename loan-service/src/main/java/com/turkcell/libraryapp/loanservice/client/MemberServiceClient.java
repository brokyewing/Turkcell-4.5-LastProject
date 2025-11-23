package com.turkcell.libraryapp.loanservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "member-service", path = "/api/students")
public interface MemberServiceClient {
    
    @GetMapping("/{id}")
    ResponseEntity<Map<String, Object>> getStudentById(@PathVariable Long id);
    
    @GetMapping("/{id}/exists")
    Boolean existsById(@PathVariable Long id);
}



