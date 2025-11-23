package com.turkcell.libraryapp.memberservice.controller;

import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.enums.MembershipLevel;
import com.turkcell.libraryapp.memberservice.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    @Autowired
    private StudentService studentService;

    @PostMapping
    public ResponseEntity<Student> createMember(@RequestBody Student request) {
        Student created = studentService.createStudent(request);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getMemberById(@PathVariable Long id) {
        Optional<Student> student = studentService.getStudentById(id);
        return student.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Student>> getMembers(
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
        return ResponseEntity.ok(students);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Student> updateMemberStatus(
            @PathVariable Long id, @RequestParam String value) {
        Student student = studentService.getStudentById(id)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        student.setMembershipLevel(MembershipLevel.valueOf(value.toUpperCase()));
        Student updated = studentService.updateStudent(id, student);
        return ResponseEntity.ok(updated);
    }
}



