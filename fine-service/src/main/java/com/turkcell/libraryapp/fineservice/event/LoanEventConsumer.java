package com.turkcell.libraryapp.fineservice.event;

import com.turkcell.libraryapp.fineservice.entity.Fine;
import com.turkcell.libraryapp.fineservice.enums.FineType;
import com.turkcell.libraryapp.fineservice.repository.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class LoanEventConsumer {

    private static final double DAILY_RATE = 5.0; // gün başına ceza (TL)

    private final FineRepository fineRepository;

    // loan-service kitap iade olayını yayınlar; fine-service burada dinler.
    // İki servis birbirini HTTP ile ÇAĞIRMAZ — sadece olay üzerinden konuşur (gevşek bağlı, asenkron).
    @KafkaListener(topics = "loan.returned", groupId = "fine-service")
    public void onLoanReturned(LoanReturnedEvent event) {
        System.out.println(">>> KAFKA'DAN ALINDI: loan.returned loanId=" + event.getLoanId());

        if (event.getReturnDate() == null || event.getDueDate() == null) {
            return;
        }
        long overdueDays = ChronoUnit.DAYS.between(event.getDueDate(), event.getReturnDate());
        if (overdueDays <= 0) {
            System.out.println(">>> Gecikme yok, ceza kesilmedi.");
            return;
        }

        // Idempotency: Kafka olayı birden fazla kez teslim edebilir (at-least-once).
        // Bu ödünç için ceza zaten varsa tekrar kesme.
        if (!fineRepository.findByLoanId(event.getLoanId()).isEmpty()) {
            System.out.println(">>> Bu ödünç için ceza zaten var, atlandı (idempotency).");
            return;
        }

        double amount = overdueDays * DAILY_RATE;
        Fine fine = new Fine(event.getStudentId(), event.getLoanId(), amount);
        fine.setFineType(FineType.LATE_RETURN);
        fine.setDailyRate(DAILY_RATE);
        fine.setCreatedDate(LocalDateTime.now());
        fineRepository.save(fine);

        System.out.println(">>> OTOMATIK CEZA KESILDI: " + amount + " TL (" + overdueDays + " gun gecikme), loanId=" + event.getLoanId());
    }
}
