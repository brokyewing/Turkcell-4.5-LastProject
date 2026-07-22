package com.turkcell.libraryapp.loanservice.client;

import com.turkcell.libraryapp.loanservice.client.fallback.BookServiceClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "book-service", path = "/api/copybooks", fallbackFactory = BookServiceClientFallbackFactory.class)
public interface BookServiceClient {

    @GetMapping("/{id}/exists")
    Boolean existsById(@PathVariable Long id);

    @GetMapping("/{id}/available")
    Boolean isAvailable(@PathVariable Long id);
}