package com.turkcell.libraryapp.memberservice.service;

import com.turkcell.libraryapp.memberservice.entity.Student;
import java.util.List;
import java.util.Optional;

public interface StudentService {
    List<Student> getAllStudents();
    Optional<Student> getStudentById(Long id);
    Student createStudent(Student student);
    Student updateStudent(Long id, Student studentDetails);
    void deleteStudent(Long id);
    List<Student> findStudentsByStatus(String status);
    List<Student> findStudentsByStatusAndEmail(String status, String email);
    List<Student> findStudentsByEmail(String email);
}



