package com.turkcell.libraryapp.fineservice.event;

import com.turkcell.libraryapp.fineservice.entity.Fine;
import com.turkcell.libraryapp.fineservice.repository.FineRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Consumer'ın idempotency davranışı: aynı loanId için ikinci kez ceza YAZILMAMALI.
 * İki katman: (1) hızlı yol findByLoanId kontrolü, (2) eşzamanlı mesajda DB unique
 * constraint'inden gelen DataIntegrityViolationException'ın yutulması.
 */
@ExtendWith(MockitoExtension.class)
class LoanEventConsumerTest {

    @Mock
    private FineRepository fineRepository;

    @InjectMocks
    private LoanEventConsumer consumer;

    @Test
    void onLoanReturned_whenFineAlreadyExists_skipsSave() {
        when(fineRepository.findByLoanId(7L)).thenReturn(List.of(new Fine(1L, 7L, 15.0)));

        consumer.onLoanReturned(event(7L, 5));

        verify(fineRepository, never()).saveAndFlush(any());
    }

    @Test
    void onLoanReturned_whenConcurrentDuplicate_violatesConstraintAndIsSwallowed() {
        when(fineRepository.findByLoanId(7L)).thenReturn(List.of());
        when(fineRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key borrowid"));

        assertDoesNotThrow(() -> consumer.onLoanReturned(event(7L, 5)));

        // Yalnızca TEK insert denemesi yapıldı (ikinci kez yazılmadı).
        verify(fineRepository, times(1)).saveAndFlush(any());
    }

    @Test
    void onLoanReturned_whenOverdue_createsSingleFineWithCorrectAmount() {
        when(fineRepository.findByLoanId(7L)).thenReturn(List.of());
        when(fineRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        consumer.onLoanReturned(event(7L, 3));

        ArgumentCaptor<Fine> captor = ArgumentCaptor.forClass(Fine.class);
        verify(fineRepository, times(1)).saveAndFlush(captor.capture());
        assertEquals(7L, captor.getValue().getLoanId());
        assertEquals(15.0, captor.getValue().getAmount()); // 3 gün × 5 TL
    }

    @Test
    void onLoanReturned_whenNotOverdue_createsNoFine() {
        consumer.onLoanReturned(event(7L, 0));

        verify(fineRepository, never()).saveAndFlush(any());
        verify(fineRepository, never()).findByLoanId(any());
    }

    /** dueDate'ten `overdueDays` gün sonra iade edilmiş bir olay üretir. */
    private LoanReturnedEvent event(Long loanId, long overdueDays) {
        LoanReturnedEvent event = new LoanReturnedEvent();
        LocalDateTime due = LocalDateTime.now().minusDays(20);
        event.setLoanId(loanId);
        event.setStudentId(1L);
        event.setDueDate(due);
        event.setReturnDate(due.plusDays(overdueDays));
        return event;
    }
}
