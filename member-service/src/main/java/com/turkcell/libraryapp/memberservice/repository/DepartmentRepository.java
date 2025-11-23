package com.turkcell.libraryapp.memberservice.repository;

import com.turkcell.libraryapp.memberservice.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
}



