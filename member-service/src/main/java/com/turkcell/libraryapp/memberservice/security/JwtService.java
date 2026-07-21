package com.turkcell.libraryapp.memberservice.security;

import com.turkcell.libraryapp.memberservice.entity.Student;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs) {
        // Aynı gizli anahtar hem burada imzalar hem gateway'de doğrular (paylaşımlı sır).
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(Student student) {
        Date now = new Date();
        return Jwts.builder()
                .subject(student.getStudentNumber())        // kimin token'ı
                .claim("studentId", student.getId())
                .claim("name", student.getName())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))  // ne zaman geçersiz
                .signWith(key)                               // imza — kurcalanırsa doğrulama patlar
                .compact();
    }
}
