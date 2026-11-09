package com.turkcell.libraryapp.memberservice.security;

import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Üretilen token, gateway'in yetkilendirme için okuduğu claim'leri taşımalı.
 */
class JwtServiceTest {

    private static final String SECRET = "library-app-super-secret-key-change-in-production-please-256bit!!";

    private final JwtService jwtService = new JwtService(SECRET, 3_600_000L);

    @Test
    void studentTokenindaRoleClaimStudent() {
        Claims claims = claimsOf(jwtService.generateToken(student(Role.STUDENT)));

        assertEquals("STUDENT", claims.get("role", String.class));
        assertEquals("S1001", claims.getSubject());
        assertEquals(5, ((Number) claims.get("studentId")).intValue());
        assertNotNull(claims.getExpiration());
    }

    @Test
    void librarianTokenindaRoleClaimLibrarian() {
        Student librarian = student(Role.LIBRARIAN);
        librarian.setStudentNumber("L1001");

        Claims claims = claimsOf(jwtService.generateToken(librarian));

        assertEquals("LIBRARIAN", claims.get("role", String.class));
        assertEquals("L1001", claims.getSubject());
    }

    // DDL güncellemesiyle gelen eski satırlarda role null olabilir → token yine STUDENT taşımalı.
    @Test
    void rolNullIseStudentVarsayilir() {
        Student legacy = student(null);

        Claims claims = claimsOf(jwtService.generateToken(legacy));

        assertEquals("STUDENT", claims.get("role", String.class));
    }

    private Student student(Role role) {
        Student student = new Student();
        student.setId(5L);
        student.setStudentNumber("S1001");
        student.setName("Ali");
        student.setRole(role);
        return student;
    }

    private Claims claimsOf(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
