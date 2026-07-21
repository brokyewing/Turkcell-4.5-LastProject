package com.turkcell.libraryapp.loanservice.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class LoanReturnRequest {
    @NotNull(message = "Loan ID is required")
    private Long loanId;
    
    private LocalDateTime returnDate;
    
    public LoanReturnRequest() {}
    
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
}







