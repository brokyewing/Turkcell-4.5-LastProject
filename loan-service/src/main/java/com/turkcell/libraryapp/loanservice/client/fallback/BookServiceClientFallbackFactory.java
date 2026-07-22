package com.turkcell.libraryapp.loanservice.client.fallback;

import com.turkcell.libraryapp.loanservice.client.BookServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookServiceClientFallbackFactory implements FallbackFactory<BookServiceClient> {

    @Override
    public BookServiceClient create(Throwable cause) {
        log.warn("BookServiceClient fallback triggered: {}", cause.getMessage());
        return new BookServiceClient() {
            @Override
            public Boolean existsById(Long id) {
                // Conservative fallback: assume book exists
                return true;
            }

            @Override
            public Boolean isAvailable(Long id) {
                // Conservative fallback: assume NOT available to prevent over-loaning
                return false;
            }
        };
    }
}