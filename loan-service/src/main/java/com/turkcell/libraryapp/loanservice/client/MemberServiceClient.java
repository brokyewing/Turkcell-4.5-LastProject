package com.turkcell.libraryapp.loanservice.client;

import com.turkcell.libraryapp.loanservice.client.fallback.MemberServiceClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "member-service", path = "/api/students", fallbackFactory = MemberServiceClientFallbackFactory.class)
public interface MemberServiceClient {

    @GetMapping("/{id}/exists")
    Boolean existsById(@PathVariable Long id);
}