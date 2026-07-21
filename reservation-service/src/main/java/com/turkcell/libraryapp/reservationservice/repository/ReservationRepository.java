package com.turkcell.libraryapp.reservationservice.repository;

import com.turkcell.libraryapp.reservationservice.entity.Reservation;
import com.turkcell.libraryapp.reservationservice.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByStudentId(Long studentId);
    List<Reservation> findByCopyBookId(Long copyBookId);
    List<Reservation> findByStatus(ReservationStatus status);
    
    @Query("SELECT r FROM Reservation r WHERE r.studentId = :studentId AND r.status = :status")
    List<Reservation> findByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") ReservationStatus status);
    
    @Query("SELECT r FROM Reservation r WHERE r.expireAt < :now AND r.status = 'ACTIVE'")
    List<Reservation> findExpiredReservations(@Param("now") LocalDateTime now);
}







