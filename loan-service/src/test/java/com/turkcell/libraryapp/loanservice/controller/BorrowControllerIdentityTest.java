package com.turkcell.libraryapp.loanservice.controller;

import com.turkcell.libraryapp.loanservice.security.IdentityGuard;
import com.turkcell.libraryapp.loanservice.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** C2: /api/borrow/me/{id} yalnız kendi id'sine. */
@WebMvcTest(BorrowController.class)
@Import(IdentityGuard.class)
class BorrowControllerIdentityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoanService loanService;

    @Test
    void studentBaskasininOdenclerini403() throws Exception {
        mockMvc.perform(get("/api/borrow/me/2")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: not your resource"));
    }

    @Test
    void studentKendiOdenclerini200() throws Exception {
        when(loanService.findLoansByMemberId(1L)).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/borrow/me/1")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isOk());
    }
}
