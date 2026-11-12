package com.turkcell.libraryapp.memberservice.controller;

import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.exception.BusinessException;
import com.turkcell.libraryapp.memberservice.repository.StudentRepository;
import com.turkcell.libraryapp.memberservice.security.JwtService;
import com.turkcell.libraryapp.memberservice.security.LoginRequest;
import com.turkcell.libraryapp.memberservice.security.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final StudentRepository studentRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        Student student = studentRepository.findByStudentNumber(request.getStudentNumber())
                .orElseThrow(() -> new BusinessException("Invalid credentials"));

        if (!passwordMatches(student, request.getPassword())) {
            // Kullanıcı-yok ve parola-yanlış aynı mesajı döner (bilgi sızdırmamak için).
            throw new BusinessException("Invalid credentials");
        }

        String token = jwtService.generateToken(student);
        String role = student.getRole() != null ? student.getRole().name() : "STUDENT";
        return new LoginResponse(token, student.getStudentNumber(), student.getName(), role);
    }

    // Yalnız BCrypt hash'i kabul edilir. Düz metin parola yolu (upgrade-on-login) KALDIRILDI:
    // o yol, başka bir uçtan düz metin yazılabilirse kalıcı bir giriş kapısına dönüşüyordu.
    private boolean passwordMatches(Student student, String rawPassword) {
        String stored = student.getPassword();
        if (stored == null || !stored.startsWith("$2")) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, stored);
    }
}
