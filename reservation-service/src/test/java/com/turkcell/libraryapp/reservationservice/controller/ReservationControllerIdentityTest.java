package com.turkcell.libraryapp.reservationservice.controller;

import com.turkcell.libraryapp.reservationservice.entity.Reservation;
import com.turkcell.libraryapp.reservationservice.enums.ReservationStatus;
import com.turkcell.libraryapp.reservationservice.security.IdentityGuard;
import com.turkcell.libraryapp.reservationservice.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C2/H3: reservation-service sahiplik kontrolü. STUDENT yalnız kendi rezervasyonunu görür;
 * "başkasının" ile "yok" aynı 404'ü döner (varlık oracle'ı kapalı).
 */
@WebMvcTest(ReservationController.class)
@Import(IdentityGuard.class)
class ReservationControllerIdentityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    private static Reservation reservationOf(Long id, Long studentId) {
        Reservation reservation = new Reservation();
        reservation.setId(id);
        reservation.setStudentId(studentId);
        reservation.setStatus(ReservationStatus.ACTIVE);
        return reservation;
    }

    @Test
    void studentBaskasininRezervasyonunu404() throws Exception {
        when(reservationService.getReservationById(9L)).thenReturn(Optional.of(reservationOf(9L, 2L)));

        mockMvc.perform(get("/api/reservations/9")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void studentKendiRezervasyonunu200() throws Exception {
        when(reservationService.getReservationById(9L)).thenReturn(Optional.of(reservationOf(9L, 1L)));

        mockMvc.perform(get("/api/reservations/9")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void studentBaskasiIcinRezervasyonYapamaz() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":2,\"copyBookId\":5}")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void studentBaskasininRezervasyonunuIptalEdemez() throws Exception {
        when(reservationService.getReservationById(9L)).thenReturn(Optional.of(reservationOf(9L, 2L)));

        mockMvc.perform(post("/api/reservations/9/cancel")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isNotFound());
    }

    // --- H3 varlık oracle'ı: "başkasının" ile "yok" AYNI yanıt ---

    @Test
    void getBaskasininIleYokAyniYanit() throws Exception {
        when(reservationService.getReservationById(9L)).thenReturn(Optional.of(reservationOf(9L, 2L)));
        MvcResult baskasi = mockMvc.perform(get("/api/reservations/9")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andReturn();

        when(reservationService.getReservationById(9L)).thenReturn(Optional.empty());
        MvcResult yok = mockMvc.perform(get("/api/reservations/9")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andReturn();

        assertThat(baskasi.getResponse().getStatus()).isEqualTo(404);
        assertThat(yok.getResponse().getStatus()).isEqualTo(404);
        assertThat(baskasi.getResponse().getContentAsString())
                .isEqualTo(yok.getResponse().getContentAsString());
    }

    @Test
    void iptalBaskasininIleYokAyniYanit() throws Exception {
        when(reservationService.getReservationById(9L)).thenReturn(Optional.of(reservationOf(9L, 2L)));
        MvcResult baskasi = mockMvc.perform(post("/api/reservations/9/cancel")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andReturn();

        when(reservationService.getReservationById(9L)).thenReturn(Optional.empty());
        MvcResult yok = mockMvc.perform(post("/api/reservations/9/cancel")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andReturn();

        assertThat(baskasi.getResponse().getStatus()).isEqualTo(404);
        assertThat(yok.getResponse().getStatus()).isEqualTo(404);
        assertThat(baskasi.getResponse().getContentAsString())
                .isEqualTo(yok.getResponse().getContentAsString());
    }
}
