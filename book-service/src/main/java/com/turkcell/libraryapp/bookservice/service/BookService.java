package com.turkcell.libraryapp.bookservice.service;

import com.turkcell.libraryapp.bookservice.dto.response.BookResponseDto;
import com.turkcell.libraryapp.bookservice.entity.Book;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookService {
    List<Book> getAllBooks();
    Optional<Book> getBookById(Long id);
    BookResponseDto getBookDtoById(Long id);   // Redis ile cache'lenen okuma
    Book createBook(Book book);
    Book updateBook(Long id, Book bookDetails);
    void deleteBook(Long id);
    boolean existsById(Long id);
    List<Book> getBooksByAuthor(Long authorId);
    List<Book> getBooksByCategory(Long categoryId);
    List<Book> findBooksByIsbn(String isbn);
    List<Book> findBooksByTitleContaining(String title);
    List<Book> findBooksByAuthor(String author);
    List<Book> findBooksWithAvailableCopies();
    Page<Book> getAllBooks(Pageable pageable);

    /**
     * Kitabın stoğunu 1 azaltır.
     * @return true → stok düştü; false → stok zaten 0 (kitap var ama müsait kopya yok)
     * @throws com.turkcell.libraryapp.bookservice.exception.BusinessException kitap hiç yoksa
     */
    boolean decrementAvailableCopies(Long bookId);

    /**
     * Kitabın stoğunu 1 artırır (iade); stok totalCopies'i aşmaz.
     * @return true → stok arttı; false → stok zaten dolu (artırılmadı)
     * @throws com.turkcell.libraryapp.bookservice.exception.BusinessException kitap hiç yoksa
     */
    boolean incrementAvailableCopies(Long bookId);
}







