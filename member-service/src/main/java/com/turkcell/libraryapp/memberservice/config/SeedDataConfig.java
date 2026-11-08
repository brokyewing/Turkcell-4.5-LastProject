package com.turkcell.libraryapp.memberservice.config;

import com.turkcell.libraryapp.memberservice.entity.Department;
import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.enums.MembershipLevel;
import com.turkcell.libraryapp.memberservice.repository.DepartmentRepository;
import com.turkcell.libraryapp.memberservice.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Uygulama boş veritabanında açıldığında demo kullanıcıyı oluşturur (S1001 / pass123).
 * Idempotent: students tablosunda kayıt varsa hiçbir şey yapmaz.
 */
@Configuration
@Profile("!test")
public class SeedDataConfig {

    @Bean
    public CommandLineRunner seedMemberData(DepartmentRepository departmentRepository,
                                            StudentRepository studentRepository,
                                            PasswordEncoder passwordEncoder) {
        return args -> {
            if (studentRepository.count() > 0) {
                return;
            }

            Department department = departmentRepository.save(
                    new Department("Bilgisayar Mühendisliği"));

            Student student = new Student();
            student.setStudentNumber("S1001");
            student.setName("Ali");
            student.setLastname("Veli");
            student.setEmail("s1001@kitaplik.local");
            student.setPhone("05000000000");
            student.setMembershipLevel(MembershipLevel.STANDARD);
            student.setDepartment(department);
            // Login BCrypt hash bekliyor (bkz. AuthController#passwordMatches).
            student.setPassword(passwordEncoder.encode("pass123"));
            studentRepository.save(student);
        };
    }
}
