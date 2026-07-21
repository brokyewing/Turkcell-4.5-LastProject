package com.turkcell.libraryapp.loanservice.event;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoanEventPublisher {

    public static final String TOPIC = "loan.returned";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishLoanReturned(LoanReturnedEvent event) {
        // key = loanId → aynı ödünçle ilgili olaylar aynı partition'a gider (sıralama korunur)
        kafkaTemplate.send(TOPIC, String.valueOf(event.getLoanId()), event);
        System.out.println(">>> KAFKA'YA YAYINLANDI: " + TOPIC + " loanId=" + event.getLoanId());
    }
}
