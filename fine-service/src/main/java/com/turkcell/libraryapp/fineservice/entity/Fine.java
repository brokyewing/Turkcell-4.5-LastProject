package com.turkcell.libraryapp.fineservice.entity;

import com.turkcell.libraryapp.fineservice.enums.FineType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fines")
public class Fine {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fineid")
    private Long id;
    
    @Column(name = "studentid", nullable = false)
    private Long studentId;
    
    @Column(name = "borrowid", nullable = false)
    private Long loanId;
    
    @Column(name = "amount", nullable = false)
    private Double amount;
    
    @Column(name = "ispaid", nullable = false)
    private Boolean isPaid = false;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "fine_type", nullable = false)
    private FineType fineType;
    
    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;
    
    @Column(name = "daily_rate")
    private Double dailyRate;
    
    public Fine() {}
    
    public Fine(Long studentId, Long loanId, Double amount) {
        this.studentId = studentId;
        this.loanId = loanId;
        this.amount = amount;
        this.isPaid = false;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }
    public FineType getFineType() { return fineType; }
    public void setFineType(FineType fineType) { this.fineType = fineType; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDateTime createdDate) { this.createdDate = createdDate; }
    public Double getDailyRate() { return dailyRate; }
    public void setDailyRate(Double dailyRate) { this.dailyRate = dailyRate; }
}







