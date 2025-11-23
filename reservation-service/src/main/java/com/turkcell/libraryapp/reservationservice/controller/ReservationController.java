package com.turkcell.libraryapp.reservationservice.controller;

import com.turkcell.libraryapp.reservationservice.dto.request.ReservationCreateRequest;
import com.turkcell.libraryapp.reservationservice.dto.response.ReservationResponseDto;
import com.turkcell.libraryapp.reservationservice.entity.Reservation;
import com.turkcell.libraryapp.reservationservice.enums.ReservationStatus;
import com.turkcell.libraryapp.reservationservice.exception.BusinessException;
import com.turkcell.libraryapp.reservationservice.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @PostMapping
    public ResponseEntity<ReservationResponseDto> createReservation(@Valid @RequestBody ReservationCreateRequest request) {
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
        Reservation reservation = reservationService.getReservationById(id)
                .orElseThrow(() -> new BusinessException("Reservation not found with id: " + id));
        
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
        return reservation.map(r -> ResponseEntity.ok(mapToDto(r)))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<ReservationResponseDto>> getAllReservations(
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String status) {
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



