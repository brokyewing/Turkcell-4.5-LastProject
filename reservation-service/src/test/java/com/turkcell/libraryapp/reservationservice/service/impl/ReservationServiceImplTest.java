package com.turkcell.libraryapp.reservationservice.service.impl;

import com.turkcell.libraryapp.reservationservice.client.BookServiceClient;
import com.turkcell.libraryapp.reservationservice.client.MemberServiceClient;
import com.turkcell.libraryapp.reservationservice.dto.response.CopyBookStockDto;
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
        when(bookServiceClient.getStock(10L)).thenReturn(new CopyBookStockDto(100L, 5));

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
        when(bookServiceClient.getStock(10L)).thenReturn(new CopyBookStockDto(100L, 0));

        assertEquals(0, reservationService.fulfillReservations());
        assertEquals(ReservationStatus.ACTIVE, active.getStatus());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void fulfillReservations_sameBookTwoCopiesOnlyOneAvailable_fulfillsOnlyOldest() {
        // BULGU-2: iki FARKLI kopya (10, 11) ama aynı kitap (100) ve stok 1.
        // Eski kod kopya bazında tekilleştirdiği için ikisini de FULFILLED yapıyordu.
        Reservation oldest = reservation(1L, 10L, ReservationStatus.ACTIVE);
        oldest.setReservationDate(LocalDateTime.now().minusDays(3));
        Reservation newer = reservation(2L, 11L, ReservationStatus.ACTIVE);
        newer.setReservationDate(LocalDateTime.now().minusDays(1));
        when(reservationRepository.findByStatus(ReservationStatus.ACTIVE))
                .thenReturn(List.of(newer, oldest));
        when(bookServiceClient.getStock(10L)).thenReturn(new CopyBookStockDto(100L, 1));
        when(bookServiceClient.getStock(11L)).thenReturn(new CopyBookStockDto(100L, 1));

        int count = reservationService.fulfillReservations();

        assertEquals(1, count);
        assertEquals(ReservationStatus.FULFILLED, oldest.getStatus());
        assertEquals(ReservationStatus.ACTIVE, newer.getStatus());
    }

    @Test
    void fulfillReservations_sameBookTwoCopiesTwoAvailable_fulfillsBoth() {
        Reservation oldest = reservation(1L, 10L, ReservationStatus.ACTIVE);
        oldest.setReservationDate(LocalDateTime.now().minusDays(3));
        Reservation newer = reservation(2L, 11L, ReservationStatus.ACTIVE);
        newer.setReservationDate(LocalDateTime.now().minusDays(1));
        when(reservationRepository.findByStatus(ReservationStatus.ACTIVE))
                .thenReturn(List.of(oldest, newer));
        when(bookServiceClient.getStock(10L)).thenReturn(new CopyBookStockDto(100L, 2));
        when(bookServiceClient.getStock(11L)).thenReturn(new CopyBookStockDto(100L, 2));

        int count = reservationService.fulfillReservations();

        assertEquals(2, count);
        assertEquals(ReservationStatus.FULFILLED, oldest.getStatus());
        assertEquals(ReservationStatus.FULFILLED, newer.getStatus());
    }

    @Test
    void fulfillReservations_twoConsecutiveTicksWithStockOne_fulfillsOnlyOneTotal() {
        // BULGU-1: tur 1'de kopya 10 FULFILLED olur; tur 2'de sayaç sıfırdan kurulmamalı,
        // aksi halde availableCopies=1 iken iki FULFILLED çıkar (turlar arası oversell).
        Reservation oldest = reservation(1L, 10L, ReservationStatus.ACTIVE);
        oldest.setReservationDate(LocalDateTime.now().minusDays(3));
        Reservation newer = reservation(2L, 11L, ReservationStatus.ACTIVE);
        newer.setReservationDate(LocalDateTime.now().minusDays(1));
        when(bookServiceClient.getStock(10L)).thenReturn(new CopyBookStockDto(100L, 1));
        when(bookServiceClient.getStock(11L)).thenReturn(new CopyBookStockDto(100L, 1));

        // Tur 1: iki kopya ACTIVE, stok 1 → yalnız en eski (kopya 10) fulfill edilir.
        when(reservationRepository.findByStatus(ReservationStatus.ACTIVE))
                .thenReturn(List.of(newer, oldest));
        assertEquals(1, reservationService.fulfillReservations());
        assertEquals(ReservationStatus.FULFILLED, oldest.getStatus());
        assertEquals(ReservationStatus.ACTIVE, newer.getStatus());

        // Tur 2: sadece kopya 11 ACTIVE kalır, stok kâğıt üzerinde yine 1.
        // Kitap 100 için 1 FULFILLED var → etkin stok 0 → ikinci rezervasyon fulfill EDİLMEMELİ.
        when(reservationRepository.findByStatus(ReservationStatus.ACTIVE))
                .thenReturn(List.of(newer));
        when(reservationRepository.countByBookIdAndStatus(100L, ReservationStatus.FULFILLED))
                .thenReturn(1L);

        assertEquals(0, reservationService.fulfillReservations());
        assertEquals(ReservationStatus.ACTIVE, newer.getStatus());
        assertEquals(ReservationStatus.FULFILLED, oldest.getStatus());
    }

    private Reservation reservation(Long id, Long copyBookId, ReservationStatus status) {
        Reservation reservation = new Reservation(5L, copyBookId, status);
        reservation.setId(id);
        reservation.setReservationDate(LocalDateTime.now().minusDays(2));
        reservation.setExpireAt(LocalDateTime.now().minusDays(1));
        return reservation;
    }
}
