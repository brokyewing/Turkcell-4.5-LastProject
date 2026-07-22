package com.turkcell.libraryapp.loanservice.client.fallback;

import com.turkcell.libraryapp.loanservice.client.MemberServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MemberServiceClientFallbackFactory implements FallbackFactory<MemberServiceClient> {

    @Override
    public MemberServiceClient create(Throwable cause) {
        log.warn("MemberServiceClient fallback triggered: {}", cause.getMessage());
        return new MemberServiceClient() {
            @Override
            public Boolean existsById(Long id) {
                // Conservative fallback: assume member exists to avoid false negatives
                return true;
            }
        };
    }
}