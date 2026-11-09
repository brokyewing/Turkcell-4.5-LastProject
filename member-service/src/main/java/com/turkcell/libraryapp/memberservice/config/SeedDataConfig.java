package com.turkcell.libraryapp.memberservice.config;

import com.turkcell.libraryapp.memberservice.entity.Department;
import com.turkcell.libraryapp.memberservice.entity.Student;
import com.turkcell.libraryapp.memberservice.enums.MembershipLevel;
import com.turkcell.libraryapp.memberservice.enums.Role;
import com.turkcell.libraryapp.memberservice.repository.DepartmentRepository;
import com.turkcell.libraryapp.memberservice.repository.StudentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Uygulama açıldığında demo kullanıcıları oluşturur:
 * S1001 / pass123 (STUDENT) ve L1001 / librarian123 (LIBRARIAN).
 * Idempotent: her kullanıcı kendi numarasına göre kontrol edilir, var olan kayda dokunulmaz.
 */
@Configuration
@Profile("!test")
public class SeedDataConfig {

    @Bean
    public CommandLineRunner seedMemberData(DepartmentRepository departmentRepository,
                                            StudentRepository studentRepository,
                                            PasswordEncoder passwordEncoder) {
        return args -> {
            seedStudent(studentRepository, departmentRepository, passwordEncoder);
            seedLibrarian(studentRepository, departmentRepository, passwordEncoder);
        };
    }

    private void seedStudent(StudentRepository studentRepository,
                             DepartmentRepository departmentRepository,
                             PasswordEncoder passwordEncoder) {
        if (studentRepository.findByStudentNumber("S1001").isPresent()) {
            return;
        }

        Department department = departmentRepository.findByName("Bilgisayar Mühendisliği")
                .orElseGet(() -> departmentRepository.save(new Department("Bilgisayar Mühendisliği")));

        Student student = new Student();
        student.setStudentNumber("S1001");
        student.setName("Ali");
        student.setLastname("Veli");
        student.setEmail("s1001@kitaplik.local");
        student.setPhone("05000000000");
        student.setMembershipLevel(MembershipLevel.STANDARD);
        student.setRole(Role.STUDENT);
        student.setDepartment(department);
        // Login BCrypt hash bekliyor (bkz. AuthController#passwordMatches).
        student.setPassword(passwordEncoder.encode("pass123"));
        studentRepository.save(student);
    }

    // Kütüphaneci: gateway'de LIBRARIAN rolüyle her yola erişebilir.
    private void seedLibrarian(StudentRepository studentRepository,
                               DepartmentRepository departmentRepository,
                               PasswordEncoder passwordEncoder) {
        if (studentRepository.findByStudentNumber("L1001").isPresent()) {
            return;
        }

        Department department = departmentRepository.findByName("Genel")
                .orElseGet(() -> departmentRepository.save(new Department("Genel")));

        Student librarian = new Student();
        librarian.setStudentNumber("L1001");
        librarian.setName("Kütüphaneci");
        librarian.setLastname("Demo");
        librarian.setEmail("l1001@kitaplik.local");
        librarian.setPhone("05000000001");
        librarian.setMembershipLevel(MembershipLevel.STANDARD);
        librarian.setRole(Role.LIBRARIAN);
        librarian.setDepartment(department);
        librarian.setPassword(passwordEncoder.encode("librarian123"));
        studentRepository.save(librarian);
    }
}
