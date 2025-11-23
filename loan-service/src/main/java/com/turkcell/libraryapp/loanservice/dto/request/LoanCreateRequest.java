package com.turkcell.libraryapp.loanservice.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class LoanCreateRequest {
    @NotNull(message = "Student ID is required")
    private Long studentId;
    
    @NotNull(message = "Copy Book ID is required")
    private Long copyBookId;
    
    private LocalDateTime loanDate;
    private LocalDateTime dueDate;
    
    public LoanCreateRequest() {}
    
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCopyBookId() { return copyBookId; }
    public void setCopyBookId(Long copyBookId) { this.copyBookId = copyBookId; }
    public LocalDateTime getLoanDate() { return loanDate; }
    public void setLoanDate(LocalDateTime loanDate) { this.loanDate = loanDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
}



