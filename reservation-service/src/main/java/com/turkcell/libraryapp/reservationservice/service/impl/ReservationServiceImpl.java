package com.turkcell.libraryapp.reservationservice.service.impl;

import com.turkcell.libraryapp.reservationservice.client.BookServiceClient;
import com.turkcell.libraryapp.reservationservice.client.MemberServiceClient;
import com.turkcell.libraryapp.reservationservice.entity.Reservation;
import com.turkcell.libraryapp.reservationservice.enums.ReservationStatus;
import com.turkcell.libraryapp.reservationservice.exception.BusinessException;
import com.turkcell.libraryapp.reservationservice.repository.ReservationRepository;
import com.turkcell.libraryapp.reservationservice.service.ReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReservationServiceImpl implements ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;
    
    @Autowired
    private MemberServiceClient memberServiceClient;
    
    @Autowired
    private BookServiceClient bookServiceClient;

    @Override
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Override
    public Optional<Reservation> getReservationById(Long id) {
        return reservationRepository.findById(id);
    }

    @Override
    public Reservation createReservation(Reservation reservation) {
        // Validate student exists
        try {
            ResponseEntity<?> studentResponse = memberServiceClient.getStudentById(reservation.getStudentId());
            if (studentResponse.getStatusCode().is4xxClientError() || !studentResponse.hasBody()) {
                throw new BusinessException("Student not found with id: " + reservation.getStudentId());
            }
        } catch (Exception e) {
            throw new BusinessException("Student not found with id: " + reservation.getStudentId());
        }
        
        // Validate copy book exists
        try {
            ResponseEntity<?> copyBookResponse = bookServiceClient.getCopyBookById(reservation.getCopyBookId());
            if (copyBookResponse.getStatusCode().is4xxClientError() || !copyBookResponse.hasBody()) {
                throw new BusinessException("CopyBook not found with id: " + reservation.getCopyBookId());
            }
        } catch (Exception e) {
            throw new BusinessException("CopyBook not found with id: " + reservation.getCopyBookId());
        }
        
        if (reservation.getReservationDate() == null) {
            reservation.setReservationDate(LocalDateTime.now());
        }
        if (reservation.getExpireAt() == null) {
            reservation.setExpireAt(reservation.getReservationDate().plusDays(7)); // Default 7 days
        }
        return reservationRepository.save(reservation);
    }

    @Override
    public Reservation updateReservation(Long id, Reservation reservationDetails) {
        Reservation existing = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Reservation not found with id: " + id));
        
        existing.setStatus(reservationDetails.getStatus());
        existing.setExpireAt(reservationDetails.getExpireAt());
        return reservationRepository.save(existing);
    }

    @Override
    public void deleteReservation(Long id) {
        if (!reservationRepository.existsById(id)) {
            throw new BusinessException("Reservation not found with id: " + id);
        }
        reservationRepository.deleteById(id);
    }

    @Override
    public List<Reservation> findReservationsByStudentId(Long studentId) {
        return reservationRepository.findByStudentId(studentId);
    }

    @Override
    public List<Reservation> findReservationsByCopyBookId(Long copyBookId) {
        return reservationRepository.findByCopyBookId(copyBookId);
    }

    @Override
    public List<Reservation> findReservationsByStatus(ReservationStatus status) {
        return reservationRepository.findByStatus(status);
    }

    @Override
    public List<Reservation> findExpiredReservations() {
        return reservationRepository.findExpiredReservations(LocalDateTime.now());
    }
}
