package com.turkcell.libraryapp.memberservice.controller;

import com.turkcell.libraryapp.memberservice.dto.StudentResponseDto;
import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.enums.MembershipLevel;
import com.turkcell.libraryapp.memberservice.exception.BusinessException;
import com.turkcell.libraryapp.memberservice.service.StudentService;
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

    @PostMapping
    public StudentResponseDto createMember(@RequestBody Student request) {
        return StudentResponseDto.from(studentService.createStudent(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDto> getMemberById(@PathVariable Long id) {
        return studentService.getStudentById(id)
                .map(s -> ResponseEntity.ok(StudentResponseDto.from(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<StudentResponseDto> getMembers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String email) {
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
        Student student = studentService.getStudentById(id)
                .orElseThrow(() -> new BusinessException("Member not found with id: " + id));
        student.setMembershipLevel(MembershipLevel.valueOf(value.toUpperCase()));
        return StudentResponseDto.from(studentService.updateStudent(id, student));
    }
}
