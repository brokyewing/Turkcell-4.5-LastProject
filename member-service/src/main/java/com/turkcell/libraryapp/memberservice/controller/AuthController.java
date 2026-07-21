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
        return new LoginResponse(token, student.getStudentNumber(), student.getName());
    }

    // BCrypt eşleşmesi. Eski düz-metin parolalar için: eşleşirse ilk girişte
    // otomatik BCrypt'e YÜKSELT (upgrade-on-login) — gerçek bir migration tekniği.
    private boolean passwordMatches(Student student, String rawPassword) {
        String stored = student.getPassword();
        if (stored == null) return false;

        if (stored.startsWith("$2")) {              // zaten BCrypt hash'i
            return passwordEncoder.matches(rawPassword, stored);
        }
        // eski düz metin
        if (stored.equals(rawPassword)) {
            student.setPassword(passwordEncoder.encode(rawPassword));
            studentRepository.save(student);        // bir daha düz metin kalmasın
            return true;
        }
        return false;
    }
}
