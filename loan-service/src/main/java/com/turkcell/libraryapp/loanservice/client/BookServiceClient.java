package com.turkcell.libraryapp.loanservice.client;

import com.turkcell.libraryapp.loanservice.client.fallback.BookServiceClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "book-service", path = "/api/copybooks", fallbackFactory = BookServiceClientFallbackFactory.class, configuration = InternalAuthFeignConfig.class)
public interface BookServiceClient {

    @GetMapping("/{id}/exists")
    Boolean existsById(@PathVariable Long id);

    @GetMapping("/{id}/available")
    Boolean isAvailable(@PathVariable Long id);

    /** Ödünç verirken stoktan 1 düşer. false → müsait kopya yok. */
    @PostMapping("/{id}/decrement")
    Boolean decrementAvailable(@PathVariable Long id);

    /** İadede stoğa 1 ekler. */
    @PostMapping("/{id}/increment")
    Boolean incrementAvailable(@PathVariable Long id);
}