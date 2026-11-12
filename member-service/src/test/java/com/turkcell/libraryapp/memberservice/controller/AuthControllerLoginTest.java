package com.turkcell.libraryapp.memberservice.controller;

import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.repository.StudentRepository;
import com.turkcell.libraryapp.memberservice.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H6: düz metin parola yolu kapatıldı. DB'de (eski kayıttan) düz metin parola kalmışsa
 * giriş REDDEDİLİR ve parola BCrypt'e yükseltilmez — upgrade-on-login kapısı yok.
 */
@WebMvcTest(AuthController.class)
class AuthControllerLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    private static Student studentWith(String storedPassword) {
        Student s = new Student();
        s.setId(1L);
        s.setStudentNumber("S1001");
        s.setName("Ada");
        s.setPassword(storedPassword);
        return s;
    }

    @Test
    void duzMetinParolaIleGirisReddedilir() throws Exception {
        when(studentRepository.findByStudentNumber("S1001"))
                .thenReturn(Optional.of(studentWith("pwned12345")));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentNumber":"S1001","password":"pwned12345"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        // Eski davranışta parola BCrypt'e yükseltilip DB'ye yazılıyordu; artık yazılmamalı.
        verify(studentRepository, never()).save(org.mockito.ArgumentMatchers.any(Student.class));
    }

    @Test
    void bcryptParolaIleGirisCalisir() throws Exception {
        String hash = "$2a$10$hashhashhashhashhashhashhashhashhashhashhashhashhash";
        when(studentRepository.findByStudentNumber("S1001"))
                .thenReturn(Optional.of(studentWith(hash)));
        when(passwordEncoder.matches("secret", hash)).thenReturn(true);
        when(jwtService.generateToken(org.mockito.ArgumentMatchers.any(Student.class))).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentNumber":"S1001","password":"secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }
}
