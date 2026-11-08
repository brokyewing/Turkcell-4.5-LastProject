package com.turkcell.libraryapp.fineservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * `fines.borrowid` üzerindeki UNIQUE index'i çalışma zamanında garanti eder.
 *
 * Neden burada: `ddl-auto=update` MEVCUT bir tabloya unique index EKLEMEZ; bu yüzden
 * entity'deki `unique = true` üretimde etkisiz kalıyordu ve ceza idempotency'si yalnız
 * uygulama seviyesindeki "önce findByLoanId" kontrolüne kalıyordu (eşzamanlı mesajlarda
 * atomik değil, `saveAndFlush` + catch ise şemada constraint olmadığı için ölü kod).
 *
 * KALICI ÇÖZÜM: Flyway migration. 8 serviste Flyway altyapısı olmadığı için bu adım
 * geçici ve idempotenttir (tekrar çalışması zararsız). Index kurulamazsa uygulama
 * düşmez; eski davranış korunur.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FineSchemaInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 1) Index'i kurmadan önce mükerrer borrowid kayıtlarını temizle.
            //    PK kolonu fineid'dir (entity: @Column(name = "fineid")).
            int deleted = jdbcTemplate.update(
                    "DELETE FROM fines a USING fines b "
                            + "WHERE a.fineid > b.fineid AND a.borrowid = b.borrowid");
            if (deleted > 0) {
                log.warn("fines: {} mükerrer borrowid kaydı silindi (unique index öncesi temizlik)", deleted);
            }

            // 2) Kısmi index: borrowid IS NULL olan satırları kapsamaz (Postgres).
            jdbcTemplate.execute(
                    "CREATE UNIQUE INDEX IF NOT EXISTS ux_fines_borrowid "
                            + "ON fines (borrowid) WHERE borrowid IS NOT NULL");

            log.info("fines.borrowid unique index hazır (ux_fines_borrowid)");
        } catch (Exception e) {
            log.error("fines.borrowid unique index kurulamadı; idempotency yalnız uygulama seviyesinde kalıyor", e);
        }
    }
}
