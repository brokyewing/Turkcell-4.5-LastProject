package com.turkcell.libraryapp.loanservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H7: aynı istemci hatası handler'ları loan-service'te de derlenip çalışıyor mu? (2. modül kanıtı)
 */
class GlobalExceptionHandlerClientErrorTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void eksikParametre400() throws Exception {
        mockMvc.perform(get("/api/test-probe/required-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required request parameter 'value' is missing"));
    }

    @Test
    void bozukGovde400VeIcTipSizmaz() throws Exception {
        mockMvc.perform(post("/api/test-probe/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"GECIKMIS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(content().string(not(containsString("GECIKMIS"))))
                .andExpect(content().string(not(containsString("ProbeStatus"))))
                .andExpect(content().string(not(containsString("HttpMessageNotReadableException"))));
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/test-probe/required-param")
        String requiredParam(@RequestParam("value") String value) {
            return value;
        }

        @PostMapping("/api/test-probe/body")
        String body(@RequestBody ProbeBody body) {
            return String.valueOf(body.getStatus());
        }
    }

    static class ProbeBody {
        private ProbeStatus status;

        public ProbeStatus getStatus() { return status; }
        public void setStatus(ProbeStatus status) { this.status = status; }
    }

    enum ProbeStatus { ZAMANINDA, COK_GEC }
}
