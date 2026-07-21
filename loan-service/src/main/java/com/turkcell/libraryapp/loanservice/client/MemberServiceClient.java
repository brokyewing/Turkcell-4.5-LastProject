package com.turkcell.libraryapp.loanservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "member-service", path = "/api/students")
public interface MemberServiceClient {

    // 200 + true/false döner (var/yok). Servis çökükse Feign hata fırlatır;
    // bu ikisini servis katmanında ayırıyoruz (bulunamadı != ulaşılamadı).
    @GetMapping("/{id}/exists")
    Boolean existsById(@PathVariable Long id);
}
