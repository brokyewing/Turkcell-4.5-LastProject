package com.turkcell.libraryapp.loanservice.controller;

import com.turkcell.libraryapp.loanservice.dto.response.LoanResponseDto;
import com.turkcell.libraryapp.loanservice.entity.Loan;
import com.turkcell.libraryapp.loanservice.security.IdentityGuard;
import com.turkcell.libraryapp.loanservice.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final LoanService loanService;
    private final IdentityGuard identityGuard;

    @GetMapping("/me/{studentId}")
    public ResponseEntity<List<LoanResponseDto>> getMyLoans(@PathVariable Long studentId) {
        // Başkasının id'si ile kendi id'si dışarıdan ayırt edilemesin diye 404 (403 değil).
        if (!canAccessOwner(studentId)) {
            return ResponseEntity.notFound().build();
        }
        List<Loan> loans = loanService.findLoansByMemberId(studentId);
        List<LoanResponseDto> responses = loans.stream()
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
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /** İç çağrı ve LIBRARIAN her kaydı görür; STUDENT yalnız kendi kaydını. */
    private boolean canAccessOwner(Long ownerStudentId) {
        if (identityGuard.isInternalCall() || identityGuard.isLibrarian()) {
            return true;
        }
        Long current = identityGuard.currentStudentId();
        return current != null && current.equals(ownerStudentId);
    }
}
