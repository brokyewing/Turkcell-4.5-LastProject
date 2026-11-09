package com.turkcell.libraryapp.reservationservice.controller;

import com.turkcell.libraryapp.reservationservice.entity.Reservation;
import com.turkcell.libraryapp.reservationservice.security.IdentityGuard;
import com.turkcell.libraryapp.reservationservice.service.ReservationService;
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

/** C2: reservation-service sahiplik kontrolü. */
@WebMvcTest(ReservationController.class)
@Import(IdentityGuard.class)
class ReservationControllerIdentityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReservationService reservationService;

    @Test
    void studentBaskasininRezervasyonunu403() throws Exception {
        Reservation reservation = new Reservation();
        reservation.setId(9L);
        reservation.setStudentId(2L);
        when(reservationService.getReservationById(9L)).thenReturn(Optional.of(reservation));

        mockMvc.perform(get("/api/reservations/9")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: not your resource"));
    }

    @Test
    void studentKendiRezervasyonunu200() throws Exception {
        Reservation reservation = new Reservation();
        reservation.setId(9L);
        reservation.setStudentId(1L);
        reservation.setStatus(com.turkcell.libraryapp.reservationservice.enums.ReservationStatus.ACTIVE);
        when(reservationService.getReservationById(9L)).thenReturn(Optional.of(reservation));

        mockMvc.perform(get("/api/reservations/9")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isOk());
    }
}
