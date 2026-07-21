package com.turkcell.libraryapp.loanservice.service;

import com.turkcell.libraryapp.loanservice.entity.Loan;
import java.time.LocalDateTime;
import java.util.List;

public interface LoanService {
    List<Loan> getAllLoans();
    Loan getLoanById(Long id);
    Loan createLoan(Loan loan);
    Loan updateLoan(Long id, Loan loanDetails);
    Loan returnLoan(Loan loan);
    void deleteLoan(Long id);
    boolean existsById(Long id);
    List<Loan> findLoansByMemberId(Long memberId);
    List<Loan> findLoansByMemberIdAndStatus(Long memberId, String status);
    List<Loan> findOverdueLoans();
    List<Loan> findLoansByDateRange(LocalDateTime startDate, LocalDateTime endDate);
}







