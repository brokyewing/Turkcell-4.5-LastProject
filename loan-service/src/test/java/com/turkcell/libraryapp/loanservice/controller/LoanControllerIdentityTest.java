package com.turkcell.libraryapp.loanservice.controller;

import com.turkcell.libraryapp.loanservice.entity.Loan;
import com.turkcell.libraryapp.loanservice.security.IdentityGuard;
import com.turkcell.libraryapp.loanservice.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C2: loan-service sahiplik kontrolü. STUDENT yalnız kendi ödünçleri üzerinde işlem yapabilir;
 * LIBRARIAN ve başlıksız (Feign) çağrılar serbesttir.
 *
 * <p>H3: Sahiplik ihlali 403 DEĞİL 404 döner; "kayıt var ama başkasının" ile "kayıt yok"
 * dışarıdan aynı yanıtı verir (varlık oracle'ı kapalı).
 */
@WebMvcTest(LoanController.class)
@Import(IdentityGuard.class)
@TestPropertySource(properties = "internal.token=test-secret")
class LoanControllerIdentityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanService loanService;

    private static final String CREATE_BODY = """
            {"studentId":2,"copyBookId":5}
            """;

    private static Loan loanOf(Long id, Long studentId) {
        Loan loan = new Loan(studentId, 5L, LocalDateTime.now());
        loan.setId(id);
        return loan;
    }

    @Test
    void studentBaskasiIcinOduncAlamaz() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY)
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void librarianBaskasiIcinOduncAlabilir() throws Exception {
        Loan loan = loanOf(100L, 2L);
        when(loanService.createLoan(any(Loan.class))).thenReturn(loan);

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY)
                        .header("X-Role", "LIBRARIAN")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentId").value(2));
    }

    @Test
    void studentBaskasininOduncunuIadeEdemez() throws Exception {
        when(loanService.findLoanById(100L)).thenReturn(Optional.of(loanOf(100L, 2L)));

        mockMvc.perform(post("/api/loans/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loanId\":100}")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void studentBaskaUyeninOdencleriniListeleyemez() throws Exception {
        mockMvc.perform(get("/api/loans/members/2")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isNotFound());
    }

    // Feign yolu (fine-service → loan-service) iç token ile gelir; bozulmamalı.
    @Test
    void existsIcTokenliCagriBozulmaz() throws Exception {
        when(loanService.existsById(100L)).thenReturn(true);

        mockMvc.perform(get("/api/loans/100/exists")
                        .header("X-Internal-Token", "test-secret"))
                .andExpect(status().isOk());
    }

    // H6: /exists varlık oracle'ı STUDENT'e kapalı.
    @Test
    void studentExistsSoramaz403() throws Exception {
        mockMvc.perform(get("/api/loans/100/exists")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isForbidden());
    }

    // --- H3 varlık oracle'ı: "başkasının" ile "yok" AYNI yanıt ---

    @Test
    void getBaskasininIleYokAyniYanit() throws Exception {
        when(loanService.findLoanById(100L)).thenReturn(Optional.of(loanOf(100L, 2L)));
        MvcResult baskasi = mockMvc.perform(get("/api/loans/100")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andReturn();

        when(loanService.findLoanById(100L)).thenReturn(Optional.empty());
        MvcResult yok = mockMvc.perform(get("/api/loans/100")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andReturn();

        assertThat(baskasi.getResponse().getStatus()).isEqualTo(404);
        assertThat(yok.getResponse().getStatus()).isEqualTo(404);
        assertThat(baskasi.getResponse().getContentAsString())
                .isEqualTo(yok.getResponse().getContentAsString());
    }

    @Test
    void iadeBaskasininIleYokAyniYanit() throws Exception {
        when(loanService.findLoanById(100L)).thenReturn(Optional.of(loanOf(100L, 2L)));
        MvcResult baskasi = mockMvc.perform(post("/api/loans/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loanId\":100}")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andReturn();

        when(loanService.findLoanById(100L)).thenReturn(Optional.empty());
        MvcResult yok = mockMvc.perform(post("/api/loans/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loanId\":100}")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andReturn();

        assertThat(baskasi.getResponse().getStatus()).isEqualTo(404);
        assertThat(yok.getResponse().getStatus()).isEqualTo(404);
        assertThat(baskasi.getResponse().getContentAsString())
                .isEqualTo(yok.getResponse().getContentAsString());
    }
}
