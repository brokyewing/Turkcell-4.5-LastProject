package com.turkcell.libraryapp.memberservice.controller;

import com.turkcell.libraryapp.memberservice.dto.StudentCreateRequest;
import com.turkcell.libraryapp.memberservice.dto.StudentResponseDto;
import com.turkcell.libraryapp.memberservice.dto.StudentUpdateRequest;
import com.turkcell.libraryapp.memberservice.entity.Department;
import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.exception.BusinessException;
import com.turkcell.libraryapp.memberservice.repository.DepartmentRepository;
import com.turkcell.libraryapp.memberservice.security.IdentityGuard;
import com.turkcell.libraryapp.memberservice.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {
    private final StudentService studentService;
    private final DepartmentRepository departmentRepository;
    private final IdentityGuard identityGuard;

    @GetMapping
    public List<StudentResponseDto> getAllStudents() {
        identityGuard.requireLibrarian();
        return studentService.getAllStudents().stream()
                .map(StudentResponseDto::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDto> getStudentById(@PathVariable Long id) {
        identityGuard.requireSelf(id);
        return studentService.getStudentById(id)
                .map(s -> ResponseEntity.ok(StudentResponseDto.from(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public StudentResponseDto addStudent(@Valid @RequestBody StudentCreateRequest request) {
        Student student = new Student();
        student.setStudentNumber(request.getStudentNumber());
        student.setName(request.getName());
        student.setLastname(request.getLastname());
        student.setEmail(request.getEmail());
        student.setPassword(request.getPassword());
        // department bilerek atanmaz: kayıt anında bölüm seçimi yok, sonradan atanır.
        return StudentResponseDto.from(studentService.createStudent(student));
    }

    // Alan listesi DTO ile sınırlıdır: password / role / studentNumber / id gövdeden ASLA alınmaz
    // (mass-assignment kapalı). Eksik alan 500 değil 400/atlanır.
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDto> updateStudent(@PathVariable Long id,
                                                           @Valid @RequestBody StudentUpdateRequest request) {
        identityGuard.requireLibrarian();
        Student student = new Student();
        student.setName(request.getName());
        student.setLastname(request.getLastname());
        student.setEmail(request.getEmail());
        student.setPhone(request.getPhone());
        student.setMembershipLevel(request.getMembershipLevel());
        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new BusinessException("Department not found with id: " + request.getDepartmentId()));
            student.setDepartment(department);
        }
        return ResponseEntity.ok(StudentResponseDto.from(studentService.updateStudent(id, student)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        identityGuard.requireLibrarian();
        studentService.deleteStudent(id);
        return ResponseEntity.ok().build();
    }

    // Servisler arası varlık kontrolü (loan/fine/reservation Feign ile çağırır).
    // Bilgi sızdırmamak için yalnız LIBRARIAN / iç çağrı: aksi halde 403.
    @GetMapping("/{id}/exists")
    public Boolean existsById(@PathVariable Long id) {
        identityGuard.requireLibrarian();
        return studentService.getStudentById(id).isPresent();
    }
}
