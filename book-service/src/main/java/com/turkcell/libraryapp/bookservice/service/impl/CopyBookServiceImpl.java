package com.turkcell.libraryapp.bookservice.service.impl;

import com.turkcell.libraryapp.bookservice.entity.CopyBook;
import com.turkcell.libraryapp.bookservice.exception.BusinessException;
import com.turkcell.libraryapp.bookservice.repository.CopyBookRepository;
import com.turkcell.libraryapp.bookservice.service.BookService;
import com.turkcell.libraryapp.bookservice.service.CopyBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CopyBookServiceImpl implements CopyBookService {

    private final CopyBookRepository copyBookRepository;
    private final BookService bookService;

    @Override
    public List<CopyBook> getAllCopyBooks() {
        return copyBookRepository.findAll();
    }

    @Override
    public Optional<CopyBook> getCopyBookById(Long id) {
        return copyBookRepository.findById(id);
    }

    @Override
    @Transactional
    public CopyBook createCopyBook(CopyBook copyBook) {
        return copyBookRepository.save(copyBook);
    }

    @Override
    @Transactional
    public void deleteCopyBook(Long id) {
        if (!copyBookRepository.existsById(id)) {
            throw new BusinessException("CopyBook not found with id: " + id);
        }
        copyBookRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return copyBookRepository.existsById(id);
    }

    @Override
    public boolean isAvailable(Long id) {
        // book-service ödünç kaydını TUTMAZ (o loan-service'in verisi). Bu yüzden burada
        // söyleyebileceğimiz tek doğru şey: kopya var mı ve bağlı olduğu kitabın stoğu var mı.
        // Kopya bazlı "şu an ödünçte mi" sorusunun sahibi loan-service'tir.
        return copyBookRepository.findById(id)
                .map(copyBook -> copyBook.getBook() != null && copyBook.getBook().getAvailableCopies() > 0)
                .orElse(false);
    }

    @Override
    public List<CopyBook> getCopyBooksByBookId(Long bookId) {
        return copyBookRepository.findByBookId(bookId);
    }

    @Override
    @Transactional
    public boolean decrementAvailableByCopyBook(Long copyBookId) {
        return bookService.decrementAvailableCopies(resolveBookId(copyBookId));
    }

    @Override
    @Transactional
    public void incrementAvailableByCopyBook(Long copyBookId) {
        bookService.incrementAvailableCopies(resolveBookId(copyBookId));
    }

    private Long resolveBookId(Long copyBookId) {
        return copyBookRepository.findById(copyBookId)
                .map(copyBook -> copyBook.getBook() != null ? copyBook.getBook().getId() : null)
                .orElseThrow(() -> new BusinessException("CopyBook not found with id: " + copyBookId));
    }
}

