package com.turkcell.libraryapp.memberservice.service.impl;

import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.enums.MembershipLevel;
import com.turkcell.libraryapp.memberservice.repository.DepartmentRepository;
import com.turkcell.libraryapp.memberservice.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * H6: updateStudent kısmi güncelleme ve parola dokunulmazlığı.
 * MemberController#updateMemberStatus aynı metodu DB'den okuduğu entity ile çağırır — o yol bozulmamalı.
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceImplUpdateTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private StudentServiceImpl service;

    private static Student existing() {
        Student s = new Student();
        s.setId(1L);
        s.setName("Ada");
        s.setLastname("Lovelace");
        s.setEmail("ada@example.com");
        s.setPhone("555");
        s.setPassword("$2a$10$existingHashExistingHashExistingHashExistingHash");
        s.setMembershipLevel(MembershipLevel.GOLD);
        return s;
    }

    @Test
    void nullAlanlarMevcutDegeriKorur() {
        Student existing = existing();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        Student patch = new Student();
        // Entity alan başlatıcısı STANDARD verir; "gönderilmedi" durumu explicit null ile temsil edilir
        // (controller DTO→entity eşlemesinde de böyle olur).
        patch.setMembershipLevel(null);
        Student saved = service.updateStudent(1L, patch);

        assertThat(saved.getName()).isEqualTo("Ada");
        assertThat(saved.getLastname()).isEqualTo("Lovelace");
        assertThat(saved.getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getPhone()).isEqualTo("555");
        assertThat(saved.getMembershipLevel()).isEqualTo(MembershipLevel.GOLD);
    }

    @Test
    void parolaAslaDegismez() {
        Student existing = existing();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        Student patch = new Student();
        patch.setPassword("pwned12345");   // doğrudan servise sızsaydı bile
        Student saved = service.updateStudent(1L, patch);

        assertThat(saved.getPassword()).isEqualTo("$2a$10$existingHashExistingHashExistingHashExistingHash");
    }

    // H8: "gönderilmedi" = null membershipLevel, mevcut GOLD'u korumalı.
    // Entity alan başlatıcısı STANDARD'dır; servisin null-guard'ı kaldırılırsa bu test kırılır.
    @Test
    void membershipLevelNullGOLDKorur() {
        Student existing = existing();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        // Controller'ın DTO→entity eşlemesi gibi: gövdede alan yoksa explicit null set edilir.
        Student patch = new Student();
        patch.setMembershipLevel(null);
        Student saved = service.updateStudent(1L, patch);

        assertThat(saved.getMembershipLevel()).isEqualTo(MembershipLevel.GOLD);
    }

    @Test
    void verilenAlanlarYazilir() {
        Student existing = existing();
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(studentRepository.save(any(Student.class))).thenAnswer(i -> i.getArgument(0));

        Student patch = new Student();
        patch.setName("Grace");
        patch.setMembershipLevel(MembershipLevel.STANDARD);
        Student saved = service.updateStudent(1L, patch);

        assertThat(saved.getName()).isEqualTo("Grace");
        assertThat(saved.getMembershipLevel()).isEqualTo(MembershipLevel.STANDARD);
        assertThat(saved.getLastname()).isEqualTo("Lovelace");
    }
}
