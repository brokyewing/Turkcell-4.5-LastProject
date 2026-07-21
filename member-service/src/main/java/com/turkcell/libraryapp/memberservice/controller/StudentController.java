package com.turkcell.libraryapp.memberservice.controller;

import com.turkcell.libraryapp.memberservice.dto.StudentResponseDto;
import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.service.StudentService;
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

    @GetMapping
    public List<StudentResponseDto> getAllStudents() {
        return studentService.getAllStudents().stream()
                .map(StudentResponseDto::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDto> getStudentById(@PathVariable Long id) {
        return studentService.getStudentById(id)
                .map(s -> ResponseEntity.ok(StudentResponseDto.from(s)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public StudentResponseDto addStudent(@RequestBody Student student) {
        return StudentResponseDto.from(studentService.createStudent(student));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDto> updateStudent(@PathVariable Long id, @RequestBody Student student) {
        return ResponseEntity.ok(StudentResponseDto.from(studentService.updateStudent(id, student)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok().build();
    }

    // Servisler arası varlık kontrolü (loan/fine/reservation Feign ile çağırır)
    @GetMapping("/{id}/exists")
    public Boolean existsById(@PathVariable Long id) {
        return studentService.getStudentById(id).isPresent();
    }
}
