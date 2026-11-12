package com.turkcell.libraryapp.memberservice.dto;

import com.turkcell.libraryapp.memberservice.enums.MembershipLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

// PUT /api/students/{id} girdisi. Tüm alanlar opsiyonel (kısmi güncelleme).
// password / role / id / studentNumber BİLEREK yok: bu uçtan parola ya da yetki yükseltilemez.
// @Size(min = 1): null (gönderilmedi) geçerli, ama boş string reddedilir — aksi halde
// {"email":""} DB'ye boş yazılıp sonraki istekte 500'e düşüyordu.
public class StudentUpdateRequest {
    @Size(min = 1, message = "name must not be empty")
    private String name;

    @Size(min = 1, message = "lastname must not be empty")
    private String lastname;

    @Email(message = "email must be a valid email address")
    @Size(min = 1, message = "email must not be empty")
    private String email;

    private String phone;
    private MembershipLevel membershipLevel;
    private Long departmentId;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public MembershipLevel getMembershipLevel() { return membershipLevel; }
    public void setMembershipLevel(MembershipLevel membershipLevel) { this.membershipLevel = membershipLevel; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
