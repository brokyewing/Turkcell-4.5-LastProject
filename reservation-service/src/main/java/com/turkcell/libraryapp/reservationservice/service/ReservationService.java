package com.turkcell.libraryapp.reservationservice.service;

import com.turkcell.libraryapp.reservationservice.entity.Reservation;
import com.turkcell.libraryapp.reservationservice.enums.ReservationStatus;
import java.util.List;
import java.util.Optional;

public interface ReservationService {
    List<Reservation> getAllReservations();
    Optional<Reservation> getReservationById(Long id);
    Reservation createReservation(Reservation reservation);
    Reservation updateReservation(Long id, Reservation reservationDetails);
    void deleteReservation(Long id);
    List<Reservation> findReservationsByStudentId(Long studentId);
    List<Reservation> findReservationsByCopyBookId(Long copyBookId);
    List<Reservation> findReservationsByStatus(ReservationStatus status);
    List<Reservation> findExpiredReservations();
}



