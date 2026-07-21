package com.turkcell.libraryapp.loanservice.service.impl;

import com.turkcell.libraryapp.loanservice.client.BookServiceClient;
import com.turkcell.libraryapp.loanservice.client.MemberServiceClient;
import com.turkcell.libraryapp.loanservice.entity.Loan;
import com.turkcell.libraryapp.loanservice.enums.LoanStatus;
import com.turkcell.libraryapp.loanservice.event.LoanEventPublisher;
import com.turkcell.libraryapp.loanservice.exception.BusinessException;
import com.turkcell.libraryapp.loanservice.repository.LoanRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import feign.FeignException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * LoanServiceImpl'in unit testleri.
 *
 * Burada gerçek veritabanı ve gerçek servisler YOK. Repository ve Feign
 * client'ların yerine Mockito'nun ürettiği sahte (mock) nesneler geçiyor.
 * Böylece sadece LoanServiceImpl'in kendi iş kurallarını test ediyoruz.
 */
@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private MemberServiceClient memberServiceClient;

    @Mock
    private BookServiceClient bookServiceClient;

    @Mock
    private LoanEventPublisher loanEventPublisher;

    @InjectMocks
    private LoanServiceImpl loanService;

    @Test
    void getLoanById_whenLoanExists_returnsLoan() {
        // Arrange: mock repository'ye "1 istenirse şu loan'ı ver" diye öğretiyoruz
        Loan loan = new Loan(10L, 20L, LocalDateTime.now());
        loan.setId(1L);
        when(loanRepository.findById(1L)).thenReturn(Optional.of(loan));

        // Act: test edilen asıl metot
        Loan result = loanService.getLoanById(1L);

        // Assert: beklenen sonuç döndü mü?
        assertEquals(1L, result.getId());
        assertEquals(10L, result.getStudentId());
    }

    @Test
    void getLoanById_whenLoanDoesNotExist_throwsBusinessException() {
        when(loanRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.getLoanById(99L));

        assertEquals("Loan not found with id: 99", ex.getMessage());
    }

    @Test
    void createLoan_whenCopyBookAlreadyLoaned_throwsBusinessException() {
        Loan loan = new Loan(10L, 20L, LocalDateTime.now());
        // Öğrenci ve kitap kopyası var gibi davran (existsById true dönsün)...
        when(memberServiceClient.existsById(10L)).thenReturn(true);
        when(bookServiceClient.existsById(20L)).thenReturn(true);
        // ...ama kopya zaten ödünçte olsun
        when(loanRepository.existsByCopyBookIdAndIsReturnedFalse(20L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.createLoan(loan));

        assertEquals("CopyBook is already loaned", ex.getMessage());
        // İş kuralı ihlalinde kayıt ASLA yapılmamalı
        verify(loanRepository, never()).save(any());
    }

    @Test
    void createLoan_whenStudentDoesNotExist_throwsBusinessException() {
        Loan loan = new Loan(99L, 20L, LocalDateTime.now());
        when(memberServiceClient.existsById(99L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> loanService.createLoan(loan));

        // Öğrenci yoksa kitap kontrolüne hiç gidilmemeli
        verify(bookServiceClient, never()).existsById(anyLong());
        verify(loanRepository, never()).save(any());
    }

    @Test
    void createLoan_whenAllValidationsPass_savesLoan() {
        Loan loan = new Loan(10L, 20L, LocalDateTime.now());
        when(memberServiceClient.existsById(10L)).thenReturn(true);
        when(bookServiceClient.existsById(20L)).thenReturn(true);
        when(loanRepository.existsByCopyBookIdAndIsReturnedFalse(20L)).thenReturn(false);
        when(loanRepository.save(loan)).thenReturn(loan);

        Loan result = loanService.createLoan(loan);

        assertNotNull(result);
        verify(loanRepository).save(loan);
    }

    @Test
    void createLoan_whenMemberServiceUnavailable_throwsUnavailableNotNotFound() {
        // BUG FİX kanıtı: member-service çökükse (Feign hatası), eski kod bunu
        // "Student not found" diye maskeliyordu. Artık ayrı, doğru hata gelmeli.
        Loan loan = new Loan(10L, 20L, LocalDateTime.now());
        FeignException down = mock(FeignException.class);
        when(down.status()).thenReturn(503);
        when(memberServiceClient.existsById(10L)).thenThrow(down);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.createLoan(loan));

        assertTrue(ex.getMessage().contains("ulaşılamadı"));
        assertFalse(ex.getMessage().contains("not found"));
        verify(loanRepository, never()).save(any());
    }

    @Test
    void returnLoan_marksLoanAsReturnedAndClosed() {
        Loan loan = new Loan(10L, 20L, LocalDateTime.now().minusDays(7));
        when(loanRepository.save(loan)).thenReturn(loan);

        Loan result = loanService.returnLoan(loan);

        assertTrue(result.getIsReturned());
        assertEquals(LoanStatus.CLOSED, result.getStatus());
        assertNotNull(result.getReturnDate());
    }

    @Test
    void findLoansByMemberIdAndStatus_withValidStatus_queriesByParsedEnum() {
        Loan loan = new Loan(10L, 20L, LocalDateTime.now());
        // Küçük harf ve boşluklu gelse bile doğru enum'a çevrilmeli
        when(loanRepository.findByStudentIdAndStatus(10L, LoanStatus.OPEN))
                .thenReturn(List.of(loan));

        List<Loan> result = loanService.findLoansByMemberIdAndStatus(10L, " open ");

        assertEquals(1, result.size());
        verify(loanRepository).findByStudentIdAndStatus(10L, LoanStatus.OPEN);
        // Eski hatalı davranış (status'u yok sayıp findByStudentId çağırmak) OLMAMALI
        verify(loanRepository, never()).findByStudentId(anyLong());
    }

    @Test
    void findLoansByMemberIdAndStatus_withInvalidStatus_throwsBusinessException() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> loanService.findLoansByMemberIdAndStatus(10L, "BANANA"));

        assertTrue(ex.getMessage().contains("Invalid loan status"));
        verify(loanRepository, never()).findByStudentIdAndStatus(anyLong(), any());
    }
}
