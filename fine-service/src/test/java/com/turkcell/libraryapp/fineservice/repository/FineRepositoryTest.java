package com.turkcell.libraryapp.fineservice.repository;

import com.turkcell.libraryapp.fineservice.entity.Fine;
import com.turkcell.libraryapp.fineservice.enums.FineType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Ceza idempotency'sinin gerçek DB'deki güvencesi: aynı ödünç (loanId) için ikinci
 * ceza kaydı, `fines.borrowid` UNIQUE constraint'i sayesinde yazılamaz.
 * Uygulama seviyesindeki "önce findByLoanId" kontrolü eşzamanlı mesajlarda yarışır;
 * constraint ise yarışmaz.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class FineRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.discovery.enabled", () -> "false");
    }

    @Autowired
    FineRepository fineRepository;

    @Test
    void secondFineForSameLoan_violatesUniqueBorrowId() {
        fineRepository.saveAndFlush(fine(100L));
        assertEquals(1, fineRepository.findByLoanId(100L).size());

        // Constraint ihlali transaction'ı rollback-only'ye çeker; sonrasında sorgu yapmıyoruz.
        assertThrows(DataIntegrityViolationException.class,
                () -> fineRepository.saveAndFlush(fine(100L)));
    }

    @Test
    void finesForDifferentLoans_areAllowed() {
        fineRepository.saveAndFlush(fine(100L));
        fineRepository.saveAndFlush(fine(101L));

        assertEquals(1, fineRepository.findByLoanId(100L).size());
        assertEquals(1, fineRepository.findByLoanId(101L).size());
    }

    private Fine fine(Long loanId) {
        Fine fine = new Fine(10L, loanId, 25.0);
        fine.setFineType(FineType.LATE_RETURN);
        fine.setDailyRate(5.0);
        fine.setCreatedDate(LocalDateTime.now());
        return fine;
    }
}
