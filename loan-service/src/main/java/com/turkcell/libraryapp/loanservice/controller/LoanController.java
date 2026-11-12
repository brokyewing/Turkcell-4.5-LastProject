package com.turkcell.libraryapp.loanservice.controller;

import com.turkcell.libraryapp.loanservice.dto.request.LoanCreateRequest;
import com.turkcell.libraryapp.loanservice.dto.request.LoanReturnRequest;
import com.turkcell.libraryapp.loanservice.dto.response.LoanResponseDto;
import com.turkcell.libraryapp.loanservice.entity.Loan;
import com.turkcell.libraryapp.loanservice.exception.BusinessException;
import com.turkcell.libraryapp.loanservice.security.IdentityGuard;
import com.turkcell.libraryapp.loanservice.service.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final IdentityGuard identityGuard;

    @PostMapping
    public ResponseEntity<LoanResponseDto> createLoan(@Valid @RequestBody LoanCreateRequest request) {
        // Sahiplik ihlali 403 değil 404 döner: başkasının studentId'si ile kendi id'si
        // dışarıdan ayırt edilemesin (varlık oracle'ı kapalı).
        if (!canAccessOwner(request.getStudentId())) {
            return ResponseEntity.notFound().build();
        }
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
        Optional<Loan> maybeLoan = loanService.findLoanById(request.getLoanId());
        if (maybeLoan.isEmpty()) {
            // Yetkili (LIBRARIAN / iç çağrı) için eski 400 davranışı korunur; STUDENT için 404.
            if (isPrivileged()) {
                throw new BusinessException("Loan not found with id: " + request.getLoanId());
            }
            return ResponseEntity.notFound().build();
        }
        Loan loan = maybeLoan.get();
        // "Yok" ile "başkasının" aynı yanıtı versin diye 404.
        if (!canAccessOwner(loan.getStudentId())) {
            return ResponseEntity.notFound().build();
        }
        
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
        if (!canAccessOwner(memberId)) {
            return ResponseEntity.notFound().build();
        }
        
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
        Optional<Loan> maybeLoan = loanService.findLoanById(id);
        // Kayıt yoksa da başkasınınsa da aynı 404 → varlık oracle'ı yok.
        if (maybeLoan.isEmpty() || !canAccessOwner(maybeLoan.get().getStudentId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mapToDto(maybeLoan.get()));
    }

    // Servisler arası varlık kontrolü için: 200 + true/false döner (fine-service Feign ile çağırır).
    @GetMapping("/{id}/exists")
    public Boolean existsById(@PathVariable Long id) {
        return loanService.existsById(id);
    }
    
    @GetMapping
    public ResponseEntity<List<LoanResponseDto>> getAllLoans() {
        identityGuard.requireLibrarian();
        List<Loan> loans = loanService.getAllLoans();
        List<LoanResponseDto> responses = loans.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
    
    /** İç çağrı ve LIBRARIAN her kaydı görür; STUDENT yalnız kendi kaydını. */
    private boolean canAccessOwner(Long ownerStudentId) {
        if (isPrivileged()) {
            return true;
        }
        Long current = identityGuard.currentStudentId();
        return current != null && current.equals(ownerStudentId);
    }

    private boolean isPrivileged() {
        return identityGuard.isInternalCall() || identityGuard.isLibrarian();
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

