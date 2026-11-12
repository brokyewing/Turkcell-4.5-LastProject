package com.turkcell.libraryapp.memberservice.controller;

import com.turkcell.libraryapp.memberservice.dto.MemberCreateRequest;
import com.turkcell.libraryapp.memberservice.dto.StudentResponseDto;
import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.enums.MembershipLevel;
import com.turkcell.libraryapp.memberservice.exception.BusinessException;
import com.turkcell.libraryapp.memberservice.security.IdentityGuard;
import com.turkcell.libraryapp.memberservice.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
    private final StudentService studentService;
    private final IdentityGuard identityGuard;

    @PostMapping
    public StudentResponseDto createMember(@Valid @RequestBody MemberCreateRequest request) {
        identityGuard.requireLibrarian();
        Student student = new Student();
        student.setStudentNumber(request.getStudentNumber());
        student.setName(request.getName());
        student.setLastname(request.getLastname());
        student.setEmail(request.getEmail());
        student.setPassword(request.getPassword());
        student.setPhone(request.getPhone());
        // role ve membershipLevel bilerek atanmaz: entity varsayılanları (STUDENT / STANDARD) kalır.
        return StudentResponseDto.from(studentService.createStudent(student));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDto> getMemberById(@PathVariable Long id) {
        identityGuard.requireSelf(id);
        return studentService.getStudentById(id)
                .map(s -> ResponseEntity.ok(StudentResponseDto.from(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<StudentResponseDto> getMembers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String email) {
        identityGuard.requireLibrarian();
        List<Student> students;
        if (status != null && email != null) {
            students = studentService.findStudentsByStatusAndEmail(status, email);
        } else if (status != null) {
            students = studentService.findStudentsByStatus(status);
        } else if (email != null) {
            students = studentService.findStudentsByEmail(email);
        } else {
            students = studentService.getAllStudents();
        }
        return students.stream().map(StudentResponseDto::from).collect(Collectors.toList());
    }

    @PatchMapping("/{id}/status")
    public StudentResponseDto updateMemberStatus(@PathVariable Long id, @RequestParam String value) {
        identityGuard.requireLibrarian();
        Student student = studentService.getStudentById(id)
                .orElseThrow(() -> new BusinessException("Member not found with id: " + id));
        student.setMembershipLevel(parseMembershipLevel(value));
        return StudentResponseDto.from(studentService.updateStudent(id, student));
    }

    // Geçersiz değer 500 değil 400 dönsün (BusinessException handler'ı BAD_REQUEST).
    private MembershipLevel parseMembershipLevel(String value) {
        try {
            return MembershipLevel.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Gecersiz uyelik seviyesi: " + value);
        }
    }
}
