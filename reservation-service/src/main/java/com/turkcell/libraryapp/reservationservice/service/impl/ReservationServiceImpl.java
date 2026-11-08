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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
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

    @Override
    @Transactional
    public int expireReservations() {
        // Sorgu zaten yalnız ACTIVE olanları getirir; yine de savunmacı süzgeç koyuyoruz.
        List<Reservation> expired = reservationRepository.findExpiredReservations(LocalDateTime.now())
                .stream()
                .filter(r -> r.getStatus() == ReservationStatus.ACTIVE)
                .toList();

        expired.forEach(r -> r.setStatus(ReservationStatus.EXPIRED));
        if (expired.isEmpty()) {
            return 0;
        }
        reservationRepository.saveAll(expired);
        return expired.size();
    }

    @Override
    @Transactional
    public int fulfillReservations() {
        List<Reservation> active = reservationRepository.findByStatus(ReservationStatus.ACTIVE);

        // Aynı kopya için birden fazla ACTIVE rezervasyon olabilir; tek kopya için
        // yalnızca en erken rezervasyon FULFILLED olmalı (yoksa aynı kopya iki kişiye gider).
        Map<Long, Reservation> firstPerCopy = new LinkedHashMap<>();
        active.stream()
                .sorted(Comparator.comparing(Reservation::getReservationDate,
                        Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())))
                .forEach(r -> firstPerCopy.putIfAbsent(r.getCopyBookId(), r));

        int fulfilled = 0;
        for (Reservation reservation : firstPerCopy.values()) {
            try {
                if (Boolean.TRUE.equals(bookServiceClient.isAvailable(reservation.getCopyBookId()))) {
                    reservation.setStatus(ReservationStatus.FULFILLED);
                    reservationRepository.save(reservation);
                    fulfilled++;
                }
            } catch (FeignException e) {
                // book-service geçici olarak yoksa bu turu atla; sonraki turda tekrar denenir.
                log.warn("book-service'e ulaşılamadı, rezervasyon fulfill edilemedi (id={}): {}",
                        reservation.getId(), e.getMessage());
            }
        }
        return fulfilled;
    }
}
