package com.turkcell.libraryapp.memberservice.dto;

import com.turkcell.libraryapp.memberservice.entity.Department;

public class DepartmentResponseDto {
    private Long id;
    private String name;

    public static DepartmentResponseDto from(Department d) {
        DepartmentResponseDto dto = new DepartmentResponseDto();
        dto.id = d.getId();
        dto.name = d.getName();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
}
