package com.turkcell.libraryapp.loanservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanEventPublisher {

    public static final String TOPIC = "loan.returned";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishLoanReturned(LoanReturnedEvent event) {
        // key = loanId → aynı ödünçle ilgili olaylar aynı partition'a gider (sıralama korunur)
        // send asenkron: sonucu dinlemezsek teslim hatası sessizce kaybolur.
        kafkaTemplate.send(TOPIC, String.valueOf(event.getLoanId()), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("loan.returned yayınlanamadı, loanId={}: {}", event.getLoanId(), ex.getMessage());
                    } else {
                        log.info("loan.returned yayınlandı, loanId={}, offset={}",
                                event.getLoanId(),
                                result != null && result.getRecordMetadata() != null
                                        ? result.getRecordMetadata().offset() : null);
                    }
                });
    }
}
