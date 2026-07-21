package com.turkcell.libraryapp.memberservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.turkcell.libraryapp.memberservice.enums.MembershipLevel;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "lastname", nullable = false)
    private String lastname;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departmantid", nullable = false)
    private Department department;
    
    // WRITE_ONLY: gelen JSON'dan OKUNUR (kayıt/giriş) ama yanıtta ASLA yazılmaz.
    // Böylece parola dışarı sızmaz. (Üretimde ayrıca BCrypt ile hash'lenmeli.)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password", nullable = false)
    private String password;
    
    @Column(name = "studentnumber", nullable = false, unique = true)
    private String studentNumber;
    
    @Column(name = "email", nullable = false, unique = true)
    private String email;
    
    @Column(name = "phone")
    private String phone;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "membership_level", nullable = false)
    private MembershipLevel membershipLevel = MembershipLevel.STANDARD;
    
    public Student() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public MembershipLevel getMembershipLevel() { return membershipLevel; }
    public void setMembershipLevel(MembershipLevel membershipLevel) { this.membershipLevel = membershipLevel; }
}







