package com.turkcell.libraryapp.memberservice.exception;

import com.turkcell.libraryapp.memberservice.enums.MembershipLevel;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * H7: istemci kaynaklı Spring MVC istisnaları 400/404 dönmeli, 500 değil.
 * Slice bağlamı kurmadan yalnız handler'ı standalone MockMvc'ye takar.
 */
class GlobalExceptionHandlerClientErrorTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ProbeController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    // a) PATCH /api/members/{id}/status value'suz: MissingServletRequestParameterException -> 400.
    @Test
    void eksikParametre400VeParametreAdi() throws Exception {
        mockMvc.perform(get("/api/test-probe/required-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required request parameter 'value' is missing"));
    }

    // b) Gövdede geçersiz enum: 400 ve iç tip adları (PREMIUM / MembershipLevel) gövdeye SIZMAMALI.
    @Test
    void bozukGovde400VeIcTipSizmaz() throws Exception {
        mockMvc.perform(post("/api/test-probe/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":\"PREMIUM\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(content().string(not(containsString("PREMIUM"))))
                .andExpect(content().string(not(containsString("MembershipLevel"))))
                .andExpect(content().string(not(containsString("HttpMessageNotReadableException"))));
    }

    // c) /api/members/abc: MethodArgumentTypeMismatchException -> 400.
    @Test
    void tipUyusmazligi400() throws Exception {
        mockMvc.perform(get("/api/test-probe/path/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for parameter 'id'"));
    }

    // d) Eşleşmeyen yol: NoHandlerFoundException -> 404 (Boot'ta pratikte ErrorController yakalar).
    @Test
    void eslesmeyenYol404() throws Exception {
        mockMvc.perform(get("/api/test-probe/no-such-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("No handler found for GET")));
    }

    @RestController
    static class ProbeController {

        @GetMapping("/api/test-probe/required-param")
        String requiredParam(@RequestParam("value") String value) {
            return value;
        }

        @PostMapping("/api/test-probe/body")
        String body(@RequestBody ProbeBody body) {
            return String.valueOf(body.getLevel());
        }

        @GetMapping("/api/test-probe/path/{id}")
        String path(@PathVariable("id") Long id) {
            return String.valueOf(id);
        }
    }

    static class ProbeBody {
        private MembershipLevel level;

        public MembershipLevel getLevel() { return level; }
        public void setLevel(MembershipLevel level) { this.level = level; }
    }
}
