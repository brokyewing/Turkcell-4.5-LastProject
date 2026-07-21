package com.turkcell.libraryapp.memberservice.service.impl;

import com.turkcell.libraryapp.memberservice.entity.Department;
import com.turkcell.libraryapp.memberservice.exception.BusinessException;
import com.turkcell.libraryapp.memberservice.repository.DepartmentRepository;
import com.turkcell.libraryapp.memberservice.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;

    @Override
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Override
    public Optional<Department> getDepartmentById(Long id) {
        return departmentRepository.findById(id);
    }

    @Override
    @Transactional
    public Department createDepartment(Department department) {
        return departmentRepository.save(department);
    }

    @Override
    @Transactional
    public Department updateDepartment(Long id, Department department) {
        Department existing = departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Department not found with id: " + id));
        existing.setName(department.getName());
        return departmentRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new BusinessException("Department not found with id: " + id);
        }
        departmentRepository.deleteById(id);
    }
}







