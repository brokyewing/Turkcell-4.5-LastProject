package com.turkcell.libraryapp.reservationservice.entity;

import com.turkcell.libraryapp.reservationservice.enums.ReservationStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservationid")
    private Long id;
    
    @Column(name = "studentid", nullable = false)
    private Long studentId;
    
    @Column(name = "copyid", nullable = false)
    private Long copyBookId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status = ReservationStatus.ACTIVE;
    
    @Column(name = "reservation_date", nullable = false)
    private LocalDateTime reservationDate;
    
    @Column(name = "expire_at")
    private LocalDateTime expireAt;
    
    public Reservation() {}
    
    public Reservation(Long studentId, Long copyBookId, ReservationStatus status) {
        this.studentId = studentId;
        this.copyBookId = copyBookId;
        this.status = status;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCopyBookId() { return copyBookId; }
    public void setCopyBookId(Long copyBookId) { this.copyBookId = copyBookId; }
    public ReservationStatus getStatus() { return status; }
    public void setStatus(ReservationStatus status) { this.status = status; }
    public LocalDateTime getReservationDate() { return reservationDate; }
    public void setReservationDate(LocalDateTime reservationDate) { this.reservationDate = reservationDate; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
}



