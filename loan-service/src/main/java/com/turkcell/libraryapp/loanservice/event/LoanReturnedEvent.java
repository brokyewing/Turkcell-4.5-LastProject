package com.turkcell.libraryapp.loanservice.event;

import java.time.LocalDateTime;

// Kitap iade edildiğinde yayınlanan olay. fine-service bunu dinleyip
// gecikme varsa ceza keser. Basit, düz alanlar (JSON'a serileştirilecek).
public class LoanReturnedEvent {
    private Long loanId;
    private Long studentId;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;

    public LoanReturnedEvent() {}

    public LoanReturnedEvent(Long loanId, Long studentId, LocalDateTime dueDate, LocalDateTime returnDate) {
        this.loanId = loanId;
        this.studentId = studentId;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
    }

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
}
