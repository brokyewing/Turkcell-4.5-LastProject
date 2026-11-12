package com.turkcell.libraryapp.bookservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H4: aynı 405 davranışı book-service'in handler'ında da derlenip çalışıyor mu?
 * Slice bağlamı kurmadan yalnız handler'ı standalone MockMvc'ye takar.
 */
class GlobalExceptionHandlerMethodNotAllowedTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new PostOnlyController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void get405VeAllowPost() throws Exception {
        mockMvc.perform(get("/api/test-only-post"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("POST")))
                .andExpect(jsonPath("$.message", containsString("Supported: POST")));
    }

    @RestController
    static class PostOnlyController {
        @PostMapping("/api/test-only-post")
        String onlyPost() {
            return "ok";
        }
    }
}
