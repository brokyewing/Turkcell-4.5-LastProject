package com.turkcell.libraryapp.memberservice.controller;

import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.security.IdentityGuard;
import com.turkcell.libraryapp.memberservice.service.StudentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * C2: member-service sahiplik kontrolü. Gateway'in eklediği X-Role / X-Student-Id başlıkları
 * taklit edilir; başlıksız istek servisler arası (Feign) çağrı sayılır.
 */
@WebMvcTest(StudentController.class)
@Import(IdentityGuard.class)
class StudentControllerIdentityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentService studentService;

    @Test
    void studentBaskaOgrenciDetayi403() throws Exception {
        mockMvc.perform(get("/api/students/2")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied: not your resource"));
    }

    @Test
    void studentKendiDetayi200() throws Exception {
        Student s = new Student();
        s.setId(1L);
        s.setName("Ada");
        s.setLastname("Lovelace");
        when(studentService.getStudentById(1L)).thenReturn(Optional.of(s));

        mockMvc.perform(get("/api/students/1")
                        .header("X-Role", "STUDENT")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // Feign yolu: loan/fine/reservation başlıksız çağırır → engellenmemeli.
    @Test
    void existsBasliksizCagriBozulmaz() throws Exception {
        when(studentService.getStudentById(2L)).thenReturn(Optional.of(new Student()));

        mockMvc.perform(get("/api/students/2/exists"))
                .andExpect(status().isOk());
    }

    @Test
    void librarianBaskaOgrenciDetayi200() throws Exception {
        when(studentService.getStudentById(2L)).thenReturn(Optional.of(new Student()));

        mockMvc.perform(get("/api/students/2")
                        .header("X-Role", "LIBRARIAN")
                        .header("X-Student-Id", "1"))
                .andExpect(status().isOk());
    }
}
