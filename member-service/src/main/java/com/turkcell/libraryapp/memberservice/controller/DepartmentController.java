package com.turkcell.libraryapp.memberservice.controller;

import com.turkcell.libraryapp.memberservice.dto.DepartmentResponseDto;
import com.turkcell.libraryapp.memberservice.entity.Department;
import com.turkcell.libraryapp.memberservice.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService departmentService;

    @GetMapping
    public List<DepartmentResponseDto> getAllDepartments() {
        return departmentService.getAllDepartments().stream()
                .map(DepartmentResponseDto::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartmentResponseDto> getDepartmentById(@PathVariable Long id) {
        return departmentService.getDepartmentById(id)
                .map(d -> ResponseEntity.ok(DepartmentResponseDto.from(d)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public DepartmentResponseDto addDepartment(@RequestBody Department department) {
        return DepartmentResponseDto.from(departmentService.createDepartment(department));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartmentResponseDto> updateDepartment(@PathVariable Long id, @RequestBody Department department) {
        return ResponseEntity.ok(DepartmentResponseDto.from(departmentService.updateDepartment(id, department)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok().build();
    }
}
