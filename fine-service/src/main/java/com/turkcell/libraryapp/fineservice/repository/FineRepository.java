package com.turkcell.libraryapp.fineservice.repository;

import com.turkcell.libraryapp.fineservice.entity.Fine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FineRepository extends JpaRepository<Fine, Long> {
    List<Fine> findByStudentId(Long studentId);
    List<Fine> findByLoanId(Long loanId);
    List<Fine> findByIsPaid(Boolean isPaid);
    
    @Query("SELECT f FROM Fine f WHERE f.studentId = :studentId AND f.isPaid = :isPaid")
    List<Fine> findByStudentIdAndIsPaid(@Param("studentId") Long studentId, @Param("isPaid") Boolean isPaid);
    
    @Query("SELECT SUM(f.amount) FROM Fine f WHERE f.studentId = :studentId AND f.isPaid = false")
    Double getTotalUnpaidFinesByStudentId(@Param("studentId") Long studentId);
}







