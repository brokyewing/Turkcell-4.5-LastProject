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
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C2/H3: fine-service sahiplik kontrolü. STUDENT yalnız kendi cezasını görür;
 * "başkasının" ile "yok" aynı 404'ü döner (varlık oracle'ı kapalı).
 */
@WebMvcTest(FineController.class)
@Import(IdentityGuard.class)
class FineControllerIdentityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FineService fineService;

    private static Fine fineOf(Long id, Long studentId) {
        Fine fine = new Fine();
        fine.setId(id);
        fine.setStudentId(studentId);
        return fine;
    }

    @Test
    void studentBaskasininCezasini404() throws Exception {
        when(fineService.getFineById(9L)).thenReturn(Optional.of(fineOf(9L, 2L)));

        mockMvc.perform(get("/api/fines/9")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isNotFound());
    }

    // Öğrenci tüm cezaları listeleyemez (studentId parametresiz) → 403 (rol uygun değil).
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

    @Test
    void studentBaskasininCezaToplami404() throws Exception {
        mockMvc.perform(get("/api/fines/students/2/total")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isNotFound());
    }

    // --- H3 varlık oracle'ı: "başkasının" ile "yok" AYNI yanıt ---

    @Test
    void cezaBaskasininIleYokAyniYanit() throws Exception {
        when(fineService.getFineById(9L)).thenReturn(Optional.of(fineOf(9L, 2L)));
        MvcResult baskasi = mockMvc.perform(get("/api/fines/9")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andReturn();

        when(fineService.getFineById(9L)).thenReturn(Optional.empty());
        MvcResult yok = mockMvc.perform(get("/api/fines/9")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andReturn();

        assertThat(baskasi.getResponse().getStatus()).isEqualTo(404);
        assertThat(yok.getResponse().getStatus()).isEqualTo(404);
        assertThat(baskasi.getResponse().getContentAsString())
                .isEqualTo(yok.getResponse().getContentAsString());
    }
}
