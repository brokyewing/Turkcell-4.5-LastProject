package com.turkcell.libraryapp.loanservice.repository;

import com.turkcell.libraryapp.loanservice.entity.Loan;
import com.turkcell.libraryapp.loanservice.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByStudentId(Long studentId);
    List<Loan> findByStudentIdAndStatus(Long studentId, LoanStatus status);
    List<Loan> findByIsReturnedFalse();
    boolean existsByCopyBookIdAndIsReturnedFalse(Long copyBookId);
    
    @Query("SELECT COUNT(l) FROM Loan l WHERE l.studentId = :studentId AND l.isReturned = false")
    long countByStudentIdAndIsReturnedFalse(@Param("studentId") Long studentId);
    
    @Query("SELECT l FROM Loan l WHERE l.isReturned = false AND l.dueDate < :currentDate")
    List<Loan> findOverdueLoans(@Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT l FROM Loan l WHERE l.loanDate BETWEEN :startDate AND :endDate")
    List<Loan> findLoansByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

