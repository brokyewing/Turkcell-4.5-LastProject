package com.turkcell.libraryapp.reservationservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "book-service", path = "/api/copybooks")
public interface BookServiceClient {

    @GetMapping("/{id}/exists")
    Boolean existsById(@PathVariable Long id);

    /** Kopyanın bağlı olduğu kitapta müsait stok var mı? (Rezervasyon fulfill kontrolü) */
    @GetMapping("/{id}/available")
    Boolean isAvailable(@PathVariable Long id);
}
