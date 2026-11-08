package com.turkcell.libraryapp.reservationservice.scheduler;

import com.turkcell.libraryapp.reservationservice.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Rezervasyon yaşam döngüsünün periyodik bakımı.
 *
 * İki iş:
 *  - expire: süresi geçmiş ACTIVE rezervasyonlar → EXPIRED
 *  - fulfill: kitabı müsait olan ACTIVE rezervasyonlar → FULFILLED
 *
 * Testlerde kapatmak için: `reservation.scheduler.enabled=false`
 * (ya da tek seferlik testte interval'i çok büyük yap).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "reservation.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class ReservationScheduler {

    private final ReservationService reservationService;

    @Scheduled(fixedDelayString = "${reservation.scheduler.expire-interval-ms:60000}")
    public void expireReservations() {
        int count = reservationService.expireReservations();
        if (count > 0) {
            log.info("{} rezervasyon süresi doldu → EXPIRED", count);
        }
    }

    @Scheduled(fixedDelayString = "${reservation.scheduler.fulfill-interval-ms:60000}")
    public void fulfillReservations() {
        int count = reservationService.fulfillReservations();
        if (count > 0) {
            log.info("{} rezervasyon kitap müsait olduğu için FULFILLED", count);
        }
    }
}
