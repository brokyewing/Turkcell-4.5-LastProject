package com.turkcell.libraryapp.memberservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Kütüphaneci tarafından üye oluşturma (POST /api/members) girdisi.
// Entity'yi doğrudan @RequestBody yapmıyoruz: role/membershipLevel/id gibi alanlar
// dışarıdan gönderilemez, böylece mass-assignment ile yetki yükseltmesi engellenir.
public class MemberCreateRequest {
    @NotBlank(message = "studentNumber must not be blank")
    private String studentNumber;

    @NotBlank(message = "name must not be blank")
    private String name;

    @NotBlank(message = "lastname must not be blank")
    private String lastname;

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotBlank(message = "password must not be blank")
    private String password;

    private String phone;

    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
