package com.turkcell.libraryapp.loanservice.controller;

import com.turkcell.libraryapp.loanservice.dto.request.LoanCreateRequest;
import com.turkcell.libraryapp.loanservice.dto.request.LoanReturnRequest;
import com.turkcell.libraryapp.loanservice.dto.response.LoanResponseDto;
import com.turkcell.libraryapp.loanservice.entity.Loan;
import com.turkcell.libraryapp.loanservice.exception.BusinessException;
import com.turkcell.libraryapp.loanservice.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    @Autowired
    private LoanService loanService;

    @PostMapping
    public ResponseEntity<LoanResponseDto> createLoan(@Valid @RequestBody LoanCreateRequest request) {
        // Business validations are done in service layer via Feign clients
        Loan loan = new Loan();
        loan.setStudentId(request.getStudentId());
        loan.setCopyBookId(request.getCopyBookId());
        loan.setLoanDate(request.getLoanDate() != null ? request.getLoanDate() : LocalDateTime.now());
        loan.setDueDate(request.getDueDate() != null ? request.getDueDate() : 
                       loan.getLoanDate().plusDays(14)); // Default 14 days
        loan.setStatus(com.turkcell.libraryapp.loanservice.enums.LoanStatus.OPEN);
        
        Loan createdLoan = loanService.createLoan(loan);
        LoanResponseDto response = mapToDto(createdLoan);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/return")
    public ResponseEntity<LoanResponseDto> returnLoan(@Valid @RequestBody LoanReturnRequest request) {
        Loan loan = loanService.getLoanById(request.getLoanId());
        
        if (loan.getIsReturned()) {
            throw new BusinessException("Loan already returned");
        }
        
        loan.setReturnDate(request.getReturnDate() != null ? request.getReturnDate() : LocalDateTime.now());
        loan.setIsReturned(true);
        
        Loan returnedLoan = loanService.returnLoan(loan);
        LoanResponseDto response = mapToDto(returnedLoan);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/members/{memberId}")
    public ResponseEntity<List<LoanResponseDto>> getMemberLoans(
            @PathVariable Long memberId,
            @RequestParam(required = false) String status) {
        
        List<Loan> loans;
        if (status != null) {
            loans = loanService.findLoansByMemberIdAndStatus(memberId, status);
        } else {
            loans = loanService.findLoansByMemberId(memberId);
        }
        
        List<LoanResponseDto> responses = loans.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponseDto> getLoanById(@PathVariable Long id) {
        Loan loan = loanService.getLoanById(id);
        LoanResponseDto response = mapToDto(loan);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<List<LoanResponseDto>> getAllLoans() {
        List<Loan> loans = loanService.getAllLoans();
        List<LoanResponseDto> responses = loans.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    private LoanResponseDto mapToDto(Loan loan) {
        LoanResponseDto dto = new LoanResponseDto();
        dto.setId(loan.getId());
        dto.setStudentId(loan.getStudentId());
        dto.setCopyBookId(loan.getCopyBookId());
        dto.setLoanDate(loan.getLoanDate());
        dto.setDueDate(loan.getDueDate());
        dto.setReturnDate(loan.getReturnDate());
        dto.setIsReturned(loan.getIsReturned());
        return dto;
    }
}

