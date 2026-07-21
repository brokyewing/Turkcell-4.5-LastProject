package com.turkcell.libraryapp.loanservice.entity;

import com.turkcell.libraryapp.loanservice.enums.LoanStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "studentcopybookloan")
public class Loan {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "studentid", nullable = false)
    private Long studentId;
    
    @Column(name = "copyid", nullable = false)
    private Long copyBookId;
    
    @Column(name = "loandate", nullable = false)
    private LocalDateTime loanDate;
    
    @Column(name = "duedate", nullable = false)
    private LocalDateTime dueDate;
    
    @Column(name = "returndate")
    private LocalDateTime returnDate;
    
    @Column(name = "isreturned", nullable = false)
    private Boolean isReturned = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LoanStatus status = LoanStatus.OPEN;
    
    public Loan() {}
    
    public Loan(Long studentId, Long copyBookId, LocalDateTime loanDate) {
        this.studentId = studentId;
        this.copyBookId = copyBookId;
        this.loanDate = loanDate;
        this.isReturned = false;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCopyBookId() { return copyBookId; }
    public void setCopyBookId(Long copyBookId) { this.copyBookId = copyBookId; }
    public LocalDateTime getLoanDate() { return loanDate; }
    public void setLoanDate(LocalDateTime loanDate) { this.loanDate = loanDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
    public Boolean getIsReturned() { return isReturned; }
    public void setIsReturned(Boolean isReturned) { this.isReturned = isReturned; }
    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }
}







