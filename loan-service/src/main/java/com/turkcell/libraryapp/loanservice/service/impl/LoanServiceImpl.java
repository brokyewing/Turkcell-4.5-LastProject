package com.turkcell.libraryapp.loanservice.service.impl;

import com.turkcell.libraryapp.loanservice.client.BookServiceClient;
import com.turkcell.libraryapp.loanservice.client.MemberServiceClient;
import com.turkcell.libraryapp.loanservice.entity.Loan;
import com.turkcell.libraryapp.loanservice.enums.LoanStatus;
import com.turkcell.libraryapp.loanservice.event.LoanEventPublisher;
import com.turkcell.libraryapp.loanservice.event.LoanReturnedEvent;
import com.turkcell.libraryapp.loanservice.exception.BusinessException;
import com.turkcell.libraryapp.loanservice.repository.LoanRepository;
import com.turkcell.libraryapp.loanservice.service.LoanService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    // Constructor injection: alanlar 'final' ve @RequiredArgsConstructor
    // Lombok'a bunlar için bir constructor ürettiriyor. Spring de o constructor'dan enjekte ediyor.
    private final LoanRepository loanRepository;
    private final MemberServiceClient memberServiceClient;
    private final BookServiceClient bookServiceClient;
    private final LoanEventPublisher loanEventPublisher;

    @Override
    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    @Override
    public Loan getLoanById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Loan not found with id: " + id));
    }

    @Override
    @Transactional
    public Loan createLoan(Loan loan) {
        ensureStudentExists(loan.getStudentId());
        ensureCopyBookExists(loan.getCopyBookId());

        // Kopya zaten ödünçteyse yeni ödünç verilemez
        if (loanRepository.existsByCopyBookIdAndIsReturnedFalse(loan.getCopyBookId())) {
            throw new BusinessException("CopyBook is already loaned");
        }

        // Stok book-service'in sahipliğinde; oradan atomik olarak düşülür.
        // Stok yoksa kayıt hiç oluşturulmaz.
        decrementStock(loan.getCopyBookId());

        // Stok AYRI bir DB'de (book-service) commit edildi. Buradan sonra bu serviste
        // bir şey patlarsa (save/flush/commit) stok düşük kalır → hayalet stok kaybı.
        // Bu yüzden herhangi bir hata durumunda stoğu geri ekleyerek telafi ediyoruz.
        AtomicBoolean compensated = new AtomicBoolean(false);
        Runnable compensate = () -> {
            if (compensated.compareAndSet(false, true)) {
                safeIncrementStock(loan.getCopyBookId());
            }
        };
        // Commit anındaki hatalar try/catch'e düşmez; rollback'te telafi buradan çalışır.
        compensateOnRollback(compensate);
        try {
            return loanRepository.save(loan);
        } catch (RuntimeException e) {
            compensate.run();
            throw e;
        }
    }

    private void ensureStudentExists(Long studentId) {
        boolean exists;
        try {
            exists = Boolean.TRUE.equals(memberServiceClient.existsById(studentId));
        } catch (FeignException e) {
            // Servis çökük / timeout / 5xx → "bulunamadı" DEĞİL, "doğrulanamadı".
            throw new BusinessException(
                    "member-service'e ulaşılamadı, öğrenci doğrulanamadı (HTTP " + e.status() + ")");
        }
        if (!exists) {
            throw new BusinessException("Student not found with id: " + studentId);
        }
    }

    private void ensureCopyBookExists(Long copyBookId) {
        boolean exists;
        try {
            exists = Boolean.TRUE.equals(bookServiceClient.existsById(copyBookId));
        } catch (FeignException e) {
            throw new BusinessException(
                    "book-service'e ulaşılamadı, kitap kopyası doğrulanamadı (HTTP " + e.status() + ")");
        }
        if (!exists) {
            throw new BusinessException("CopyBook not found with id: " + copyBookId);
        }
    }

    private void decrementStock(Long copyBookId) {
        boolean decremented;
        try {
            decremented = Boolean.TRUE.equals(bookServiceClient.decrementAvailable(copyBookId));
        } catch (FeignException e) {
            // Feign hatası = "ulaşılamadı". Stok düşülemediği için ödünç verilmez.
            throw new BusinessException(
                    "book-service'e ulaşılamadı, stok düşülemedi (HTTP " + e.status() + ")");
        }
        if (!decremented) {
            // false iki durumdan gelebilir: gerçekten stok yok VEYA book-service'e
            // ulaşılamadı (fallback false döner). Kesin nedeni bilmediğimiz için mesaj
            // ikisini de kapsar; "Copy is not available" diyip nedeni maskeliyorduk.
            throw new BusinessException(
                    "Stok düşülemedi: kopya müsait değil ya da book-service yanıt vermedi (copyBookId="
                            + copyBookId + ")");
        }
    }

    private void incrementStock(Long copyBookId) {
        boolean incremented;
        try {
            incremented = Boolean.TRUE.equals(bookServiceClient.incrementAvailable(copyBookId));
        } catch (FeignException e) {
            throw new BusinessException(
                    "book-service'e ulaşılamadı, stok artırılamadı (HTTP " + e.status() + ")");
        }
        if (!incremented) {
            throw new BusinessException("Stok artırılamadı (book-service başarısız yanıt): " + copyBookId);
        }
    }

    /**
     * Transaction rollback'e (commit hatası dahil) düşerse stok telafisini çalıştırır.
     * Aktif transaction yoksa (ör. unit test) boş döner; çağıran try/catch halleder.
     */
    private void compensateOnRollback(Runnable compensation) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    compensation.run();
                }
            }
        });
    }

    /**
     * Stok telafisi (increment). Telafi çağrısının kendisi patlarsa orijinal istisnayı
     * MASKELEMEZ: loglar ve devam eder.
     */
    private void safeIncrementStock(Long copyBookId) {
        try {
            Boolean ok = bookServiceClient.incrementAvailable(copyBookId);
            if (!Boolean.TRUE.equals(ok)) {
                log.error("Stok telafisi etkisiz kaldı (increment false), copyBookId={}", copyBookId);
            }
        } catch (RuntimeException e) {
            log.error("Stok telafisi başarısız (increment, copyBookId={}): {}", copyBookId, e.getMessage());
        }
    }

    /**
     * Stok telafisi (decrement) — iade akışında stok fazla artırıldıysa geri düşürür.
     */
    private void safeDecrementStock(Long copyBookId) {
        try {
            Boolean ok = bookServiceClient.decrementAvailable(copyBookId);
            if (!Boolean.TRUE.equals(ok)) {
                log.error("Stok telafisi etkisiz kaldı (decrement false), copyBookId={}", copyBookId);
            }
        } catch (RuntimeException e) {
            log.error("Stok telafisi başarısız (decrement, copyBookId={}): {}", copyBookId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public Loan updateLoan(Long id, Loan loanDetails) {
        Loan existingLoan = getLoanById(id);
        existingLoan.setReturnDate(loanDetails.getReturnDate());
        existingLoan.setIsReturned(loanDetails.getIsReturned());
        existingLoan.setStatus(loanDetails.getStatus());
        return loanRepository.save(existingLoan);
    }

    @Override
    @Transactional
    public Loan returnLoan(Loan loan) {
        // Stok önce artırılır: book-service erişilemezse iade reddedilir (stok sızmasın).
        incrementStock(loan.getCopyBookId());

        // Tersi yön: stok artırıldıktan sonra bu serviste bir şey patlarsa (save/commit)
        // stok şişer ve totalCopies aşılabilir. Hata olursa stok geri düşürülür.
        AtomicBoolean compensated = new AtomicBoolean(false);
        Runnable compensate = () -> {
            if (compensated.compareAndSet(false, true)) {
                safeDecrementStock(loan.getCopyBookId());
            }
        };
        compensateOnRollback(compensate);
        try {
            loan.setReturnDate(LocalDateTime.now());
            loan.setIsReturned(true);
            loan.setStatus(LoanStatus.CLOSED);
            Loan saved = loanRepository.save(loan);

            // Olayı yayınla — fine-service asenkron dinleyip gecikme cezası kesecek.
            // loan-service, cezanın kesilip kesilmediğini BİLMEZ ve UMURSAMAZ (gevşek bağlılık).
            // Commit'ten ÖNCE yayınlarsak transaction geri alındığında "hayalet" olay kalır;
            // bu yüzden yayın afterCommit'e ertelenir.
            publishAfterCommit(new LoanReturnedEvent(
                    saved.getId(), saved.getStudentId(), saved.getDueDate(), saved.getReturnDate()));
            return saved;
        } catch (RuntimeException e) {
            compensate.run();
            throw e;
        }
    }

    private void publishAfterCommit(LoanReturnedEvent event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Aktif transaction yok (ör. unit test) → doğrudan yayınla.
            loanEventPublisher.publishLoanReturned(event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                loanEventPublisher.publishLoanReturned(event);
            }
        });
    }

    @Override
    @Transactional
    public void deleteLoan(Long id) {
        if (!loanRepository.existsById(id)) {
            throw new BusinessException("Loan not found with id: " + id);
        }
        loanRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return loanRepository.existsById(id);
    }

    @Override
    public List<Loan> findLoansByMemberId(Long memberId) {
        return loanRepository.findByStudentId(memberId);
    }

    @Override
    public List<Loan> findLoansByMemberIdAndStatus(Long memberId, String status) {
        // Gelen String status'u enum'a çevir; geçersizse anlamlı bir hata fırlat.
        LoanStatus loanStatus;
        try {
            loanStatus = LoanStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(
                    "Invalid loan status: " + status + ". Valid values: OPEN, CLOSED, LATE");
        }
        return loanRepository.findByStudentIdAndStatus(memberId, loanStatus);
    }

    @Override
    public List<Loan> findOverdueLoans() {
        return loanRepository.findOverdueLoans(LocalDateTime.now());
    }

    @Override
    public List<Loan> findLoansByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return loanRepository.findLoansByDateRange(startDate.toLocalDate(), endDate.toLocalDate());
    }
}
