package com.turkcell.libraryapp.reservationservice.dto.response;

import java.time.LocalDateTime;

public class ReservationResponseDto {
    private Long reservationId;
    private Long studentId;
    private Long copyBookId;
    private String status;
    private LocalDateTime reservationDate;
    private LocalDateTime expireAt;
    
    public ReservationResponseDto() {}
    
    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCopyBookId() { return copyBookId; }
    public void setCopyBookId(Long copyBookId) { this.copyBookId = copyBookId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getReservationDate() { return reservationDate; }
    public void setReservationDate(LocalDateTime reservationDate) { this.reservationDate = reservationDate; }
    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }
}



