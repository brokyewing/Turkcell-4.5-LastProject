package com.turkcell.libraryapp.memberservice.dto;

import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.enums.MembershipLevel;

// Öğrencinin dış (API) temsili. password YOK, department döngüsü YOK.
// Entity'yi hiç serileştirmediğimiz için @JsonIgnore/@JsonIgnoreProperties yamalarına gerek kalmaz.
public class StudentResponseDto {
    private Long id;
    private String name;
    private String lastname;
    private Long departmentId;
    private String departmentName;
    private String studentNumber;
    private String email;
    private String phone;
    private MembershipLevel membershipLevel;

    public static StudentResponseDto from(Student s) {
        StudentResponseDto d = new StudentResponseDto();
        d.id = s.getId();
        d.name = s.getName();
        d.lastname = s.getLastname();
        if (s.getDepartment() != null) {
            d.departmentId = s.getDepartment().getId();
            d.departmentName = s.getDepartment().getName();
        }
        d.studentNumber = s.getStudentNumber();
        d.email = s.getEmail();
        d.phone = s.getPhone();
        d.membershipLevel = s.getMembershipLevel();
        return d;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getLastname() { return lastname; }
    public Long getDepartmentId() { return departmentId; }
    public String getDepartmentName() { return departmentName; }
    public String getStudentNumber() { return studentNumber; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public MembershipLevel getMembershipLevel() { return membershipLevel; }
}
