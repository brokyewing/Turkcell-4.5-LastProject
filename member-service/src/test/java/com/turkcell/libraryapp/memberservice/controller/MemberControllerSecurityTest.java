package com.turkcell.libraryapp.memberservice.controller;

import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.enums.MembershipLevel;
import com.turkcell.libraryapp.memberservice.enums.Role;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H2: /api/members uçlarının yetki kontrolü ve mass-assignment koruması.
 * Gateway'in eklediği X-Role / X-Student-Id başlıkları taklit edilir (başlıksız istek = Feign/iç çağrı).
 */
@WebMvcTest(MemberController.class)
@Import(IdentityGuard.class)
class MemberControllerSecurityTest {

    private static final String VALID_BODY = """
            {"studentNumber":"S1","name":"Ada","lastname":"Lovelace",
             "email":"ada@example.com","password":"secret","phone":"555"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    private static Student student(long id) {
        Student s = new Student();
        s.setId(id);
        s.setName("Ada");
        s.setLastname("Lovelace");
        return s;
    }

    // a) Regresyon: eskiden guard yoktu, STUDENT 200 alıyordu.
    @Test
    void studentUyeOlusturamaz403() throws Exception {
        mockMvc.perform(post("/api/members")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    // b) Body'de role gönderilse bile entity LIBRARIAN'a yükselmemeli.
    @Test
    void bodydekiRoleYokSayilir() throws Exception {
        when(studentService.createStudent(any(Student.class))).thenReturn(student(10L));

        mockMvc.perform(post("/api/members")
                        .header("X-Role", "LIBRARIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"id":99,"studentNumber":"S1","name":"Ada","lastname":"Lovelace",
                                 "email":"ada@example.com","password":"secret",
                                 "role":"LIBRARIAN","membershipLevel":"GOLD","department":{"id":1}}
                                """))
                .andExpect(status().isOk());

        ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
        verify(studentService).createStudent(captor.capture());
        Student created = captor.getValue();
        assertThat(created.getRole()).isEqualTo(Role.STUDENT);
        assertThat(created.getMembershipLevel()).isEqualTo(MembershipLevel.STANDARD);
        assertThat(created.getId()).isNull();
    }

    @Test
    void gecersizGovde400() throws Exception {
        mockMvc.perform(post("/api/members")
                        .header("X-Role", "LIBRARIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // c) Başka üyenin detayı 403; kendi detayı 403 OLMAMALI (kayıt yoksa 404).
    @Test
    void studentBaskaUyeDetayi403() throws Exception {
        mockMvc.perform(get("/api/members/2")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studentKendiDetayi403Degil() throws Exception {
        when(studentService.getStudentById(1L)).thenReturn(Optional.of(student(1L)));

        mockMvc.perform(get("/api/members/1")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // d) Tüm üye listesi yalnız kütüphaneciye.
    @Test
    void studentUyeListesiniGoremez403() throws Exception {
        mockMvc.perform(get("/api/members")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isForbidden());
    }

    // e) Üyelik seviyesi değişikliği yalnız kütüphaneciye.
    @Test
    void studentUyelikSeviyesiDegistiremez403() throws Exception {
        mockMvc.perform(patch("/api/members/1/status")
                        .param("value", "GOLD")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void gecersizUyelikSeviyesi400() throws Exception {
        when(studentService.getStudentById(1L)).thenReturn(Optional.of(student(1L)));

        mockMvc.perform(patch("/api/members/1/status")
                        .param("value", "PLATINUM")
                        .header("X-Role", "LIBRARIAN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Gecersiz uyelik seviyesi: PLATINUM"));
    }

    // f) LIBRARIAN tüm uçlarda geçer.
    @Test
    void librarianTumUclardaGecer() throws Exception {
        when(studentService.createStudent(any(Student.class))).thenReturn(student(10L));
        when(studentService.getStudentById(2L)).thenReturn(Optional.of(student(2L)));
        when(studentService.getAllStudents()).thenReturn(List.of(student(2L)));
        when(studentService.updateStudent(eq(2L), any(Student.class))).thenReturn(student(2L));

        mockMvc.perform(post("/api/members")
                        .header("X-Role", "LIBRARIAN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/members/2").header("X-Role", "LIBRARIAN"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/members").header("X-Role", "LIBRARIAN"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/members/2/status")
                        .param("value", "gold")
                        .header("X-Role", "LIBRARIAN"))
                .andExpect(status().isOk());
    }
}
