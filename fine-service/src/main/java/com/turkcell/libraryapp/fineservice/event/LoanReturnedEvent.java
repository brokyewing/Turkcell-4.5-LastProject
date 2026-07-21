package com.turkcell.libraryapp.fineservice.event;

import java.time.LocalDateTime;

// loan-service'in yayınladığı olayın fine-service tarafındaki karşılığı.
// Aynı alanlar; iki servis birbirinin sınıfını PAYLAŞMAZ (gevşek bağlılık).
public class LoanReturnedEvent {
    private Long loanId;
    private Long studentId;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;

    public LoanReturnedEvent() {}

    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
}
