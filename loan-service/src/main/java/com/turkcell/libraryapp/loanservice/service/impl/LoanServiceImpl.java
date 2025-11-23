package com.turkcell.libraryapp.loanservice.service.impl;

import com.turkcell.libraryapp.loanservice.client.BookServiceClient;
import com.turkcell.libraryapp.loanservice.client.MemberServiceClient;
import com.turkcell.libraryapp.loanservice.entity.Loan;
import com.turkcell.libraryapp.loanservice.exception.BusinessException;
import com.turkcell.libraryapp.loanservice.repository.LoanRepository;
import com.turkcell.libraryapp.loanservice.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanServiceImpl implements LoanService {

    @Autowired
    private LoanRepository loanRepository;
    
    @Autowired
    private MemberServiceClient memberServiceClient;
    
    @Autowired
    private BookServiceClient bookServiceClient;

    @Override
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    @Override
    public Loan getLoanById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Loan not found with id: " + id));
    }

    @Override
    public Loan createLoan(Loan loan) {
        // Validate student exists
        try {
            ResponseEntity<?> studentResponse = memberServiceClient.getStudentById(loan.getStudentId());
            if (studentResponse.getStatusCode().is4xxClientError() || !studentResponse.hasBody()) {
                throw new BusinessException("Student not found with id: " + loan.getStudentId());
            }
        } catch (Exception e) {
            throw new BusinessException("Student not found with id: " + loan.getStudentId());
        }
        
        // Validate copy book exists
        try {
            ResponseEntity<?> copyBookResponse = bookServiceClient.getCopyBookById(loan.getCopyBookId());
            if (copyBookResponse.getStatusCode().is4xxClientError() || !copyBookResponse.hasBody()) {
                throw new BusinessException("CopyBook not found with id: " + loan.getCopyBookId());
            }
        } catch (Exception e) {
            throw new BusinessException("CopyBook not found with id: " + loan.getCopyBookId());
        }
        
        // Check if copy book is already loaned
        boolean alreadyLoaned = loanRepository.existsByCopyBookIdAndIsReturnedFalse(loan.getCopyBookId());
        if (alreadyLoaned) {
            throw new BusinessException("CopyBook is already loaned");
        }
        
        return loanRepository.save(loan);
    }

    @Override
    public Loan updateLoan(Long id, Loan loanDetails) {
        Loan existingLoan = getLoanById(id);
        existingLoan.setReturnDate(loanDetails.getReturnDate());
        existingLoan.setIsReturned(loanDetails.getIsReturned());
        existingLoan.setStatus(loanDetails.getStatus());
        return loanRepository.save(existingLoan);
    }

    @Override
    public Loan returnLoan(Loan loan) {
        loan.setReturnDate(LocalDateTime.now());
        loan.setIsReturned(true);
        loan.setStatus(com.turkcell.libraryapp.loanservice.enums.LoanStatus.CLOSED);
        return loanRepository.save(loan);
    }

    @Override
    public void deleteLoan(Long id) {
        if (!loanRepository.existsById(id)) {
            throw new BusinessException("Loan not found with id: " + id);
        }
        loanRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return loanRepository.existsById(id);
    }

    @Override
    public List<Loan> findLoansByMemberId(Long memberId) {
        return loanRepository.findByStudentId(memberId);
    }

    @Override
    public List<Loan> findLoansByMemberIdAndStatus(Long memberId, String status) {
        return loanRepository.findByStudentId(memberId); // Simplified
    }

    @Override
    public List<Loan> findOverdueLoans() {
        return loanRepository.findOverdueLoans(LocalDateTime.now());
    }

    @Override
    public List<Loan> findLoansByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return loanRepository.findLoansByDateRange(startDate.toLocalDate(), endDate.toLocalDate());
    }
}
