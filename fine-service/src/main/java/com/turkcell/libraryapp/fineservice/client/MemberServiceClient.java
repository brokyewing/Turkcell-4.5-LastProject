package com.turkcell.libraryapp.fineservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "member-service", path = "/api/students")
public interface MemberServiceClient {

    @GetMapping("/{id}/exists")
    Boolean existsById(@PathVariable Long id);
}
