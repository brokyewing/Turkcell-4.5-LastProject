package com.turkcell.libraryapp.memberservice.repository;

import com.turkcell.libraryapp.memberservice.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByEmail(String email);
    Optional<Student> findByStudentNumber(String studentNumber);
    
    @Query("SELECT s FROM Student s WHERE s.membershipLevel = :status")
    List<Student> findByStatus(@Param("status") String status);
    
    @Query("SELECT s FROM Student s WHERE s.email = :email AND s.membershipLevel = :status")
    List<Student> findByStatusAndEmail(@Param("status") String status, @Param("email") String email);
    
    List<Student> findByEmailContaining(String email);
}



