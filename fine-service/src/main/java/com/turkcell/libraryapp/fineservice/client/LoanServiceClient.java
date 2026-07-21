package com.turkcell.libraryapp.fineservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "loan-service", path = "/api/loans")
public interface LoanServiceClient {

    @GetMapping("/{id}/exists")
    Boolean existsById(@PathVariable Long id);
}
