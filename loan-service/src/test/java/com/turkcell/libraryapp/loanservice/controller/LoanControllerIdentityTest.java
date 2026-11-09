package com.turkcell.libraryapp.loanservice.controller;

import com.turkcell.libraryapp.loanservice.entity.Loan;
import com.turkcell.libraryapp.loanservice.security.IdentityGuard;
import com.turkcell.libraryapp.loanservice.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C2: loan-service sahiplik kontrolü. STUDENT yalnız kendi ödünçleri üzerinde işlem yapabilir;
 * LIBRARIAN ve başlıksız (Feign) çağrılar serbesttir.
 */
@WebMvcTest(LoanController.class)
@Import(IdentityGuard.class)
class LoanControllerIdentityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanService loanService;

    private static final String CREATE_BODY = """
            {"studentId":2,"copyBookId":5}
            """;

    @Test
    void studentBaskasiIcinOduncAlamaz() throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY)
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: not your resource"));
    }

    @Test
    void librarianBaskasiIcinOduncAlabilir() throws Exception {
        Loan loan = new Loan(2L, 5L, LocalDateTime.now());
        loan.setId(100L);
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
        Loan loan = new Loan(2L, 5L, LocalDateTime.now());
        loan.setId(100L);
        when(loanService.getLoanById(100L)).thenReturn(loan);

        mockMvc.perform(post("/api/loans/return")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"loanId\":100}")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentBaskaUyeninOdencleriniListeleyemez() throws Exception {
        mockMvc.perform(get("/api/loans/members/2")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isForbidden());
    }

    // Feign yolu (fine-service → loan-service) başlıksız gelir; bozulmamalı.
    @Test
    void existsBasliksizCagriBozulmaz() throws Exception {
        when(loanService.existsById(100L)).thenReturn(true);

        mockMvc.perform(get("/api/loans/100/exists"))
                .andExpect(status().isOk());
    }
}
