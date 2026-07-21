package com.turkcell.libraryapp.reservationservice.service.impl;

import com.turkcell.libraryapp.reservationservice.client.BookServiceClient;
import com.turkcell.libraryapp.reservationservice.client.MemberServiceClient;
import com.turkcell.libraryapp.reservationservice.entity.Reservation;
import com.turkcell.libraryapp.reservationservice.enums.ReservationStatus;
import com.turkcell.libraryapp.reservationservice.exception.BusinessException;
import com.turkcell.libraryapp.reservationservice.repository.ReservationRepository;
import com.turkcell.libraryapp.reservationservice.service.ReservationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository reservationRepository;
    
    private final MemberServiceClient memberServiceClient;
    
    private final BookServiceClient bookServiceClient;

    @Override
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Override
    public Optional<Reservation> getReservationById(Long id) {
        return reservationRepository.findById(id);
    }

    @Override
    @Transactional
    public Reservation createReservation(Reservation reservation) {
        ensureStudentExists(reservation.getStudentId());
        ensureCopyBookExists(reservation.getCopyBookId());

        if (reservation.getReservationDate() == null) {
            reservation.setReservationDate(LocalDateTime.now());
        }
        if (reservation.getExpireAt() == null) {
            reservation.setExpireAt(reservation.getReservationDate().plusDays(7)); // Default 7 days
        }
        return reservationRepository.save(reservation);
    }

    private void ensureStudentExists(Long studentId) {
        boolean exists;
        try {
            exists = Boolean.TRUE.equals(memberServiceClient.existsById(studentId));
        } catch (FeignException e) {
            throw new BusinessException(
                    "member-service'e ulaşılamadı, öğrenci doğrulanamadı (HTTP " + e.status() + ")");
        }
        if (!exists) {
            throw new BusinessException("Student not found with id: " + studentId);
        }
    }

    private void ensureCopyBookExists(Long copyBookId) {
        boolean exists;
        try {
            exists = Boolean.TRUE.equals(bookServiceClient.existsById(copyBookId));
        } catch (FeignException e) {
            throw new BusinessException(
                    "book-service'e ulaşılamadı, kitap kopyası doğrulanamadı (HTTP " + e.status() + ")");
        }
        if (!exists) {
            throw new BusinessException("CopyBook not found with id: " + copyBookId);
        }
    }

    @Override
    @Transactional
    public Reservation updateReservation(Long id, Reservation reservationDetails) {
        Reservation existing = reservationRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Reservation not found with id: " + id));
        
        existing.setStatus(reservationDetails.getStatus());
        existing.setExpireAt(reservationDetails.getExpireAt());
        return reservationRepository.save(existing);
    }

    @Override
    @Transactional
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
