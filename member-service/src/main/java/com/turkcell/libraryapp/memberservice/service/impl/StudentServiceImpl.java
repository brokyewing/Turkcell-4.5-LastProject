package com.turkcell.libraryapp.memberservice.service.impl;

import com.turkcell.libraryapp.memberservice.entity.Department;
import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.exception.BusinessException;
import com.turkcell.libraryapp.memberservice.repository.DepartmentRepository;
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
    private final DepartmentRepository departmentRepository;
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
        // Bölüm seçilmediyse varsayılan "Genel" bölümüne düşür — NOT NULL kısıtı doğal olarak sağlanır,
        // yönetici sonradan değiştirebilir.
        if (student.getDepartment() == null) {
            student.setDepartment(departmentRepository.findByName("Genel")
                    .orElseGet(() -> departmentRepository.save(new Department("Genel"))));
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
        // Kısmi güncelleme: yalnız gönderilen (null olmayan) alanlar yazılır — NOT NULL alanlar
        // eksik gövdeyle silinip 500'e düşmez.
        if (studentDetails.getName() != null) {
            existing.setName(studentDetails.getName());
        }
        if (studentDetails.getLastname() != null) {
            existing.setLastname(studentDetails.getLastname());
        }
        if (studentDetails.getEmail() != null) {
            existing.setEmail(studentDetails.getEmail());
        }
        if (studentDetails.getPhone() != null) {
            existing.setPhone(studentDetails.getPhone());
        }
        // Parola bu uçtan ASLA değişmez: studentDetails.getPassword() bilerek kullanılmıyor.
        if (studentDetails.getMembershipLevel() != null) {
            existing.setMembershipLevel(studentDetails.getMembershipLevel());
        }
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







