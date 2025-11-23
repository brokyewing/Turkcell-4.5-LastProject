package com.turkcell.libraryapp.fineservice.service.impl;

import com.turkcell.libraryapp.fineservice.client.LoanServiceClient;
import com.turkcell.libraryapp.fineservice.client.MemberServiceClient;
import com.turkcell.libraryapp.fineservice.entity.Fine;
import com.turkcell.libraryapp.fineservice.exception.BusinessException;
import com.turkcell.libraryapp.fineservice.repository.FineRepository;
import com.turkcell.libraryapp.fineservice.service.FineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class FineServiceImpl implements FineService {

    @Autowired
    private FineRepository fineRepository;
    
    @Autowired
    private MemberServiceClient memberServiceClient;
    
    @Autowired
    private LoanServiceClient loanServiceClient;

    @Override
    public List<Fine> getAllFines() {
        return fineRepository.findAll();
    }

    @Override
    public Optional<Fine> getFineById(Long id) {
        return fineRepository.findById(id);
    }

    @Override
    public Fine createFine(Fine fine) {
        // Validate student exists
        try {
            ResponseEntity<?> studentResponse = memberServiceClient.getStudentById(fine.getStudentId());
            if (studentResponse.getStatusCode().is4xxClientError() || !studentResponse.hasBody()) {
                throw new BusinessException("Student not found with id: " + fine.getStudentId());
            }
        } catch (Exception e) {
            throw new BusinessException("Student not found with id: " + fine.getStudentId());
        }
        
        // Validate loan exists
        try {
            ResponseEntity<?> loanResponse = loanServiceClient.getLoanById(fine.getLoanId());
            if (loanResponse.getStatusCode().is4xxClientError() || !loanResponse.hasBody()) {
                throw new BusinessException("Loan not found with id: " + fine.getLoanId());
            }
        } catch (Exception e) {
            throw new BusinessException("Loan not found with id: " + fine.getLoanId());
        }
        
        if (fine.getCreatedDate() == null) {
            fine.setCreatedDate(LocalDateTime.now());
        }
        return fineRepository.save(fine);
    }

    @Override
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
