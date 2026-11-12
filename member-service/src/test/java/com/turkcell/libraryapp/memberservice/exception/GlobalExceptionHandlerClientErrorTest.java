package com.turkcell.libraryapp.memberservice.exception;

import com.turkcell.libraryapp.memberservice.enums.MembershipLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
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
 * H7/H8: istemci kaynaklı Spring MVC istisnaları 400/404 dönmeli, 500 değil.
 *
 * <p>H8: gerçek DispatcherServlet + gerçek ResourceHandler üzerinden çalışır
 * ({@code @WebMvcTest}). Eskiden {@code standaloneSetup} kullanılıyordu; o kurulum
 * eşleşmeyen yolda {@code NoHandlerFoundException} fırlatırken üretim (Boot 3.2+)
 * {@code NoResourceFoundException} fırlatıyor — test yeşil, üretim 500 idi.
 */
@WebMvcTest(controllers = ProbeController.class)
class GlobalExceptionHandlerClientErrorTest {

    @Autowired
    private MockMvc mockMvc;

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

    // d) Hiçbir handler'a eşleşmeyen yol: üretimde NoResourceFoundException -> 404, iç yol SIZMAMALI.
    @Test
    void eslesmeyenYol404VeIcYolSizmaz() throws Exception {
        mockMvc.perform(get("/api/test-probe/no-such-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No such endpoint"))
                .andExpect(content().string(not(containsString("No static resource"))))
                .andExpect(content().string(not(containsString("NoResourceFound"))))
                .andExpect(content().string(not(containsString("api/test-probe/no-such-endpoint"))));
    }
}

@RestController
class ProbeController {

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

class ProbeBody {
    private MembershipLevel level;

    public MembershipLevel getLevel() { return level; }
    public void setLevel(MembershipLevel level) { this.level = level; }
}
