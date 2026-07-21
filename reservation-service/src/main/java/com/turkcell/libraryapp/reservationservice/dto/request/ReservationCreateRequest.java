package com.turkcell.libraryapp.reservationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class ReservationCreateRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;
    
    @NotNull(message = "Copy Book ID is required")
    private Long copyBookId;
    
    private LocalDateTime reservationDate;
    private LocalDateTime expireAt;
    
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCopyBookId() { return copyBookId; }
    public void setCopyBookId(Long copyBookId) { this.copyBookId = copyBookId; }
    public LocalDateTime getReservationDate() { return reservationDate; }
    public void setReservationDate(LocalDateTime reservationDate) { this.reservationDate = reservationDate; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
}







