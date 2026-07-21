package com.turkcell.libraryapp.fineservice.service;

import com.turkcell.libraryapp.fineservice.entity.Fine;
import java.util.List;
import java.util.Optional;

public interface FineService {
    List<Fine> getAllFines();
    Optional<Fine> getFineById(Long id);
    Fine createFine(Fine fine);
    Fine updateFine(Long id, Fine fineDetails);
    void deleteFine(Long id);
    List<Fine> findFinesByStudentId(Long studentId);
    List<Fine> findFinesByLoanId(Long loanId);
    List<Fine> findUnpaidFines();
    List<Fine> findFinesByStudentIdAndIsPaid(Long studentId, Boolean isPaid);
    Double getTotalUnpaidFinesByStudentId(Long studentId);
    Fine payFine(Long id);
}







