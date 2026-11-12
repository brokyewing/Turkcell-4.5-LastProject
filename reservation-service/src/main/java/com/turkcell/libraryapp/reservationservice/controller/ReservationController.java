package com.turkcell.libraryapp.reservationservice.controller;

import com.turkcell.libraryapp.reservationservice.dto.request.ReservationCreateRequest;
import com.turkcell.libraryapp.reservationservice.dto.response.ReservationResponseDto;
import com.turkcell.libraryapp.reservationservice.entity.Reservation;
import com.turkcell.libraryapp.reservationservice.enums.ReservationStatus;
import com.turkcell.libraryapp.reservationservice.exception.BusinessException;
import com.turkcell.libraryapp.reservationservice.security.IdentityGuard;
import com.turkcell.libraryapp.reservationservice.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;
    private final IdentityGuard identityGuard;

    @PostMapping
    public ResponseEntity<ReservationResponseDto> createReservation(@Valid @RequestBody ReservationCreateRequest request) {
        // Sahiplik ihlali 403 değil 404 → başkasının studentId'si kendi id'sinden ayırt edilemesin.
        if (!canAccessOwner(request.getStudentId())) {
            return ResponseEntity.notFound().build();
        }
        Reservation reservation = new Reservation();
        reservation.setStudentId(request.getStudentId());
        reservation.setCopyBookId(request.getCopyBookId());
        reservation.setReservationDate(request.getReservationDate() != null ? 
                                     request.getReservationDate() : java.time.LocalDateTime.now());
        reservation.setExpireAt(request.getExpireAt());
        reservation.setStatus(ReservationStatus.ACTIVE);
        
        Reservation createdReservation = reservationService.createReservation(reservation);
        ReservationResponseDto response = mapToDto(createdReservation);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponseDto> cancelReservation(@PathVariable Long id) {
        Optional<Reservation> maybeReservation = reservationService.getReservationById(id);
        if (maybeReservation.isEmpty()) {
            // Yetkili (LIBRARIAN / iç çağrı) için eski 400 davranışı korunur; STUDENT için 404.
            if (isPrivileged()) {
                throw new BusinessException("Reservation not found with id: " + id);
            }
            return ResponseEntity.notFound().build();
        }
        Reservation reservation = maybeReservation.get();
        // "Yok" ile "başkasının" aynı yanıtı versin diye 404.
        if (!canAccessOwner(reservation.getStudentId())) {
            return ResponseEntity.notFound().build();
        }
        
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new BusinessException("Only active reservations can be cancelled");
        }
        
        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation cancelledReservation = reservationService.updateReservation(id, reservation);
        ReservationResponseDto response = mapToDto(cancelledReservation);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponseDto> getReservationById(@PathVariable Long id) {
        Optional<Reservation> reservation = reservationService.getReservationById(id);
        // Kayıt yoksa da başkasınınsa da aynı 404 → varlık oracle'ı yok.
        if (reservation.isEmpty() || !canAccessOwner(reservation.get().getStudentId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToDto(reservation.get()));
    }
    
    @GetMapping
    public ResponseEntity<List<ReservationResponseDto>> getAllReservations(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String status) {
        identityGuard.requireLibrarian();
        List<Reservation> reservations;
        if (studentId != null) {
            reservations = reservationService.findReservationsByStudentId(studentId);
        } else if (status != null) {
            reservations = reservationService.findReservationsByStatus(ReservationStatus.valueOf(status.toUpperCase()));
        } else {
            reservations = reservationService.getAllReservations();
        }
        
        List<ReservationResponseDto> responses = reservations.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    /** İç çağrı ve LIBRARIAN her kaydı görür; STUDENT yalnız kendi kaydını. */
    private boolean canAccessOwner(Long ownerStudentId) {
        if (isPrivileged()) {
            return true;
        }
        Long current = identityGuard.currentStudentId();
        return current != null && current.equals(ownerStudentId);
    }

    private boolean isPrivileged() {
        return identityGuard.isInternalCall() || identityGuard.isLibrarian();
    }

    private ReservationResponseDto mapToDto(Reservation reservation) {
        ReservationResponseDto dto = new ReservationResponseDto();
        dto.setReservationId(reservation.getId());
        dto.setStudentId(reservation.getStudentId());
        dto.setCopyBookId(reservation.getCopyBookId());
        dto.setStatus(reservation.getStatus().toString());
        dto.setReservationDate(reservation.getReservationDate());
        dto.setExpireAt(reservation.getExpireAt());
        return dto;
    }
}







