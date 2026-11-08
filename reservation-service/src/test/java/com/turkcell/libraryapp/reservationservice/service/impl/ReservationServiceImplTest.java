package com.turkcell.libraryapp.reservationservice.service.impl;

import com.turkcell.libraryapp.reservationservice.client.BookServiceClient;
import com.turkcell.libraryapp.reservationservice.client.MemberServiceClient;
import com.turkcell.libraryapp.reservationservice.entity.Reservation;
import com.turkcell.libraryapp.reservationservice.enums.ReservationStatus;
import com.turkcell.libraryapp.reservationservice.repository.ReservationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Zamanlanmış rezervasyon işlerinin saf birim testleri (DB/Feign yok).
 */
@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private MemberServiceClient memberServiceClient;

    @Mock
    private BookServiceClient bookServiceClient;

    @InjectMocks
    private ReservationServiceImpl reservationService;

    @Test
    void expireReservations_marksOnlyActiveExpiredOnesAsExpired() {
        Reservation active = reservation(1L, 10L, ReservationStatus.ACTIVE);
        Reservation expired = reservation(2L, 10L, ReservationStatus.EXPIRED);
        // Sorgu normalde yalnız ACTIVE döner; savunmacı süzgeci de sınıyoruz.
        when(reservationRepository.findExpiredReservations(any(LocalDateTime.class)))
                .thenReturn(List.of(active, expired));

        int count = reservationService.expireReservations();

        assertEquals(1, count);
        assertEquals(ReservationStatus.EXPIRED, active.getStatus());
        assertEquals(ReservationStatus.EXPIRED, expired.getStatus());
        // Yalnız ACTIVE olan kayıt yazılır.
        verify(reservationRepository).saveAll(List.of(active));
    }

    @Test
    void expireReservations_whenNothingExpired_savesNothing() {
        when(reservationRepository.findExpiredReservations(any(LocalDateTime.class)))
                .thenReturn(List.of());

        assertEquals(0, reservationService.expireReservations());
        verify(reservationRepository, never()).saveAll(any());
    }

    @Test
    void fulfillReservations_whenCopyAvailable_marksOldestReservationPerCopyFulfilled() {
        Reservation oldest = reservation(1L, 10L, ReservationStatus.ACTIVE);
        oldest.setReservationDate(LocalDateTime.now().minusDays(3));
        Reservation newer = reservation(2L, 10L, ReservationStatus.ACTIVE);
        newer.setReservationDate(LocalDateTime.now().minusDays(1));
        when(reservationRepository.findByStatus(ReservationStatus.ACTIVE))
                .thenReturn(List.of(newer, oldest));
        when(bookServiceClient.isAvailable(10L)).thenReturn(true);

        int count = reservationService.fulfillReservations();

        // Aynı kopya için yalnızca bir rezervasyon fulfill edilmeli.
        assertEquals(1, count);
        assertEquals(ReservationStatus.FULFILLED, oldest.getStatus());
        assertEquals(ReservationStatus.ACTIVE, newer.getStatus());
    }

    @Test
    void fulfillReservations_whenCopyNotAvailable_leavesActive() {
        Reservation active = reservation(1L, 10L, ReservationStatus.ACTIVE);
        active.setReservationDate(LocalDateTime.now().minusDays(1));
        when(reservationRepository.findByStatus(ReservationStatus.ACTIVE)).thenReturn(List.of(active));
        when(bookServiceClient.isAvailable(10L)).thenReturn(false);

        assertEquals(0, reservationService.fulfillReservations());
        assertEquals(ReservationStatus.ACTIVE, active.getStatus());
        verify(reservationRepository, never()).save(any());
    }

    private Reservation reservation(Long id, Long copyBookId, ReservationStatus status) {
        Reservation reservation = new Reservation(5L, copyBookId, status);
        reservation.setId(id);
        reservation.setReservationDate(LocalDateTime.now().minusDays(2));
        reservation.setExpireAt(LocalDateTime.now().minusDays(1));
        return reservation;
    }
}
