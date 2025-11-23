package com.turkcell.libraryapp.loanservice.controller;

import com.turkcell.libraryapp.loanservice.dto.response.LoanResponseDto;
import com.turkcell.libraryapp.loanservice.entity.Loan;
import com.turkcell.libraryapp.loanservice.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/borrow")
public class BorrowController {

    @Autowired
    private LoanService loanService;

    @GetMapping("/me/{studentId}")
    public List<LoanResponseDto> getMyLoans(@PathVariable Long studentId) {
        List<Loan> loans = loanService.findLoansByMemberId(studentId);
        return loans.stream()
                .map(loan -> {
                    LoanResponseDto dto = new LoanResponseDto();
                    dto.setId(loan.getId());
                    dto.setStudentId(loan.getStudentId());
                    dto.setCopyBookId(loan.getCopyBookId());
                    dto.setLoanDate(loan.getLoanDate());
                    dto.setDueDate(loan.getDueDate());
                    dto.setReturnDate(loan.getReturnDate());
                    dto.setIsReturned(loan.getIsReturned());
                    return dto;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}



