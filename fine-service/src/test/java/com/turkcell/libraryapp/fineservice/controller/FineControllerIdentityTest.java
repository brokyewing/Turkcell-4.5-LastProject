package com.turkcell.libraryapp.fineservice.controller;

import com.turkcell.libraryapp.fineservice.entity.Fine;
import com.turkcell.libraryapp.fineservice.security.IdentityGuard;
import com.turkcell.libraryapp.fineservice.service.FineService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** C2: fine-service sahiplik kontrolü. */
@WebMvcTest(FineController.class)
@Import(IdentityGuard.class)
class FineControllerIdentityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FineService fineService;

    @Test
    void studentBaskasininCezasini403() throws Exception {
        Fine fine = new Fine();
        fine.setId(9L);
        fine.setStudentId(2L);
        when(fineService.getFineById(9L)).thenReturn(Optional.of(fine));

        mockMvc.perform(get("/api/fines/9")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: not your resource"));
    }

    // Öğrenci tüm cezaları listeleyemez (studentId parametresiz) → 403.
    @Test
    void studentTumCezalariListeleyemez() throws Exception {
        mockMvc.perform(get("/api/fines")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentKendiCezaToplami200() throws Exception {
        when(fineService.getTotalUnpaidFinesByStudentId(1L)).thenReturn(0.0);

        mockMvc.perform(get("/api/fines/students/1/total")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isOk());
    }
}
