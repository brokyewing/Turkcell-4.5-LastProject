package com.turkcell.libraryapp.memberservice.controller;

import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.enums.Role;
import com.turkcell.libraryapp.memberservice.repository.DepartmentRepository;
import com.turkcell.libraryapp.memberservice.security.IdentityGuard;
import com.turkcell.libraryapp.memberservice.service.StudentService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H6: PUT /api/students/{id} mass-assignment regresyonu.
 *
 * <p>Eskiden gövde doğrudan {@code Student} entity'sine bağlanıyordu; {@code password} düz metin
 * olarak DB'ye yazılıyor (hesap devralma) ve {@code role} yükseltilebiliyordu. Artık gövde
 * {@link com.turkcell.libraryapp.memberservice.dto.StudentUpdateRequest} ile sınırlıdır.
 */
@WebMvcTest(StudentController.class)
@Import(IdentityGuard.class)
class StudentControllerUpdateSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @MockitoBean
    private DepartmentRepository departmentRepository;

    private static Student existing() {
        Student s = new Student();
        s.setId(1L);
        s.setName("Ada");
        s.setLastname("Lovelace");
        s.setEmail("ada@example.com");
        s.setPassword("$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashhash");
        s.setRole(Role.STUDENT);
        return s;
    }

    // a) Regresyon: gövdedeki password servise ULAŞMAMALI (eskiden düz metin yazılıyordu).
    @Test
    void passwordGovdedenServiseUlasmaz() throws Exception {
        when(studentService.updateStudent(eq(1L), any(Student.class))).thenReturn(existing());

        mockMvc.perform(put("/api/students/1")
                        .header("X-Role", "LIBRARIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada","lastname":"Lovelace","email":"ada@example.com",
                                 "password":"pwned12345"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentService).updateStudent(eq(1L), captor.capture());
        assertThat(captor.getValue().getPassword()).isNull();
    }

    // b) Gövdedeki role yok sayılır (DTO'da alan yok → entity varsayılanı STUDENT).
    @Test
    void roleGovdedenYukseltilemez() throws Exception {
        when(studentService.updateStudent(eq(1L), any(Student.class))).thenReturn(existing());

        mockMvc.perform(put("/api/students/1")
                        .header("X-Role", "LIBRARIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada","role":"LIBRARIAN","studentNumber":"S999"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentService).updateStudent(eq(1L), captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.STUDENT);
        assertThat(captor.getValue().getStudentNumber()).isNull();
    }

    // c) Gönderilmeyen alan null kalır; servis mevcut değeri korur (StudentServiceImplUpdateTest).
    @Test
    void gonderilmeyenAlanlarNullGider() throws Exception {
        when(studentService.updateStudent(eq(1L), any(Student.class))).thenReturn(existing());

        mockMvc.perform(put("/api/students/1")
                        .header("X-Role", "LIBRARIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"phone":"555"}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentService).updateStudent(eq(1L), captor.capture());
        assertThat(captor.getValue().getName()).isNull();
        assertThat(captor.getValue().getPhone()).isEqualTo("555");
        // H8: membershipLevel da explicit null gitmeli; servis "null'sa koru" kuralıyla çalışır.
        // Controller'da setMembershipLevel(request.getMembershipLevel()) satırı silinirse entity
        // başlatıcısı STANDARD verir ve mevcut GOLD sessizce düşer — bu assert onu yakalar.
        assertThat(captor.getValue().getMembershipLevel()).isNull();
    }

    // d) Geçersiz email 500 değil 400.
    @Test
    void gecersizEmail400() throws Exception {
        mockMvc.perform(put("/api/students/1")
                        .header("X-Role", "LIBRARIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // e) H8: boş email 400 — eskiden DB'ye boş yazılıp ikinci istekte 500'e düşüyordu.
    @Test
    void bosEmail400() throws Exception {
        mockMvc.perform(put("/api/students/1")
                        .header("X-Role", "LIBRARIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":""}
                                """))
                .andExpect(status().isBadRequest());
    }

    // Yetki kontrolü korunuyor: STUDENT bu ucu kullanamaz.
    @Test
    void studentGuncelleyemez403() throws Exception {
        mockMvc.perform(put("/api/students/1")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Mallory"}
                                """))
                .andExpect(status().isForbidden());
    }
}
