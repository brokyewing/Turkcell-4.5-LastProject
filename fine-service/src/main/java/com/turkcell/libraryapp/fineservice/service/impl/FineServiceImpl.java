package com.turkcell.libraryapp.fineservice.service.impl;

import com.turkcell.libraryapp.fineservice.client.LoanServiceClient;
import com.turkcell.libraryapp.fineservice.client.MemberServiceClient;
import com.turkcell.libraryapp.fineservice.entity.Fine;
import com.turkcell.libraryapp.fineservice.exception.BusinessException;
import com.turkcell.libraryapp.fineservice.repository.FineRepository;
import com.turkcell.libraryapp.fineservice.service.FineService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FineServiceImpl implements FineService {

    private final FineRepository fineRepository;
    
    private final MemberServiceClient memberServiceClient;
    
    private final LoanServiceClient loanServiceClient;

    @Override
    public List<Fine> getAllFines() {
        return fineRepository.findAll();
    }

    @Override
    public Optional<Fine> getFineById(Long id) {
        return fineRepository.findById(id);
    }

    @Override
    @Transactional
    public Fine createFine(Fine fine) {
        ensureStudentExists(fine.getStudentId());
        ensureLoanExists(fine.getLoanId());

        if (fine.getCreatedDate() == null) {
            fine.setCreatedDate(LocalDateTime.now());
        }
        return fineRepository.save(fine);
    }

    private void ensureStudentExists(Long studentId) {
        boolean exists;
        try {
            exists = Boolean.TRUE.equals(memberServiceClient.existsById(studentId));
        } catch (FeignException e) {
            throw new BusinessException(
                    "member-service'e ulaşılamadı, öğrenci doğrulanamadı (HTTP " + e.status() + ")");
        }
        if (!exists) {
            throw new BusinessException("Student not found with id: " + studentId);
        }
    }

    private void ensureLoanExists(Long loanId) {
        boolean exists;
        try {
            exists = Boolean.TRUE.equals(loanServiceClient.existsById(loanId));
        } catch (FeignException e) {
            throw new BusinessException(
                    "loan-service'e ulaşılamadı, ödünç doğrulanamadı (HTTP " + e.status() + ")");
        }
        if (!exists) {
            throw new BusinessException("Loan not found with id: " + loanId);
        }
    }

    @Override
    @Transactional
    public Fine updateFine(Long id, Fine fineDetails) {
        Fine existing = fineRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Fine not found with id: " + id));
        
        existing.setAmount(fineDetails.getAmount());
        existing.setIsPaid(fineDetails.getIsPaid());
        existing.setFineType(fineDetails.getFineType());
        existing.setDailyRate(fineDetails.getDailyRate());
        return fineRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteFine(Long id) {
        if (!fineRepository.existsById(id)) {
            throw new BusinessException("Fine not found with id: " + id);
        }
        fineRepository.deleteById(id);
    }

    @Override
    public List<Fine> findFinesByStudentId(Long studentId) {
        return fineRepository.findByStudentId(studentId);
    }

    @Override
    public List<Fine> findFinesByLoanId(Long loanId) {
        return fineRepository.findByLoanId(loanId);
    }

    @Override
    public List<Fine> findUnpaidFines() {
        return fineRepository.findByIsPaid(false);
    }

    @Override
    public List<Fine> findFinesByStudentIdAndIsPaid(Long studentId, Boolean isPaid) {
        return fineRepository.findByStudentIdAndIsPaid(studentId, isPaid);
    }

    @Override
    public Double getTotalUnpaidFinesByStudentId(Long studentId) {
        Double total = fineRepository.getTotalUnpaidFinesByStudentId(studentId);
        return total != null ? total : 0.0;
    }

    @Override
    public Fine payFine(Long id) {
        Fine fine = fineRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Fine not found with id: " + id));
        
        if (fine.getIsPaid()) {
            throw new BusinessException("Fine already paid");
        }
        
        fine.setIsPaid(true);
        return fineRepository.save(fine);
    }
}
