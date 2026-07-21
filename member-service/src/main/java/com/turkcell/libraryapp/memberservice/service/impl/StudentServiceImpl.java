package com.turkcell.libraryapp.memberservice.service.impl;

import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.exception.BusinessException;
import com.turkcell.libraryapp.memberservice.repository.StudentRepository;
import com.turkcell.libraryapp.memberservice.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Override
    public Optional<Student> getStudentById(Long id) {
        return studentRepository.findById(id);
    }

    @Override
    @Transactional
    public Student createStudent(Student student) {
        if (studentRepository.findByEmail(student.getEmail()).isPresent()) {
            throw new BusinessException("Student with email " + student.getEmail() + " already exists");
        }
        if (studentRepository.findByStudentNumber(student.getStudentNumber()).isPresent()) {
            throw new BusinessException("Student with student number " + student.getStudentNumber() + " already exists");
        }
        // Parolayı DB'ye hash'lenmiş yaz — düz metin ASLA saklanmaz.
        student.setPassword(passwordEncoder.encode(student.getPassword()));
        return studentRepository.save(student);
    }

    @Override
    @Transactional
    public Student updateStudent(Long id, Student studentDetails) {
        Student existing = studentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Student not found with id: " + id));
        existing.setName(studentDetails.getName());
        existing.setLastname(studentDetails.getLastname());
        existing.setEmail(studentDetails.getEmail());
        existing.setPhone(studentDetails.getPhone());
        existing.setPassword(studentDetails.getPassword());
        existing.setMembershipLevel(studentDetails.getMembershipLevel());
        if (studentDetails.getDepartment() != null) {
            existing.setDepartment(studentDetails.getDepartment());
        }
        return studentRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteStudent(Long id) {
        if (!studentRepository.existsById(id)) {
            throw new BusinessException("Student not found with id: " + id);
        }
        studentRepository.deleteById(id);
    }

    @Override
    public List<Student> findStudentsByStatus(String status) {
        return studentRepository.findByStatus(status);
    }

    @Override
    public List<Student> findStudentsByStatusAndEmail(String status, String email) {
        return studentRepository.findByStatusAndEmail(status, email);
    }

    @Override
    public List<Student> findStudentsByEmail(String email) {
        return studentRepository.findByEmailContaining(email);
    }
}







