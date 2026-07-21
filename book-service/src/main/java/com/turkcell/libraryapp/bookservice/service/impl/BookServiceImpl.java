package com.turkcell.libraryapp.bookservice.service.impl;

import com.turkcell.libraryapp.bookservice.dto.response.BookResponseDto;
import com.turkcell.libraryapp.bookservice.entity.Book;
import com.turkcell.libraryapp.bookservice.exception.BusinessException;
import com.turkcell.libraryapp.bookservice.repository.BookRepository;
import com.turkcell.libraryapp.bookservice.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    @Override
    @Cacheable(value = "books", key = "#id")
    public BookResponseDto getBookDtoById(Long id) {
        // Bu gövde yalnızca cache MISS'te çalışır (DB'ye gidilir).
        // Cache HIT'te Spring proxy metodu hiç çağırmaz, sonucu doğrudan Redis'ten döndürür.
        System.out.println(">>> DB'DEN OKUNDU (cache MISS): book id=" + id);
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Book not found with id: " + id));
        return toDto(book);
    }

    @Override
    @Transactional
    public Book createBook(Book book) {
        return bookRepository.save(book);
    }

    @Override
    @Transactional
    @CacheEvict(value = "books", key = "#id")   // güncellenince eski cache'i sil
    public Book updateBook(Long id, Book bookDetails) {
        Optional<Book> optionalBook = bookRepository.findById(id);
        if (optionalBook.isPresent()) {
            Book book = optionalBook.get();
            book.setTitle(bookDetails.getTitle());
            book.setIsbn(bookDetails.getIsbn());
            book.setAuthor(bookDetails.getAuthor());
            book.setCategory(bookDetails.getCategory());
            book.setPublicationDate(bookDetails.getPublicationDate());
            book.setTotalCopies(bookDetails.getTotalCopies());
            book.setAvailableCopies(bookDetails.getAvailableCopies());
            book.setStatus(bookDetails.getStatus());
            return bookRepository.save(book);
        }
        throw new BusinessException("Book not found with id: " + id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "books", key = "#id")   // silinince cache'ten de düş
    public void deleteBook(Long id) {
        if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id);
        } else {
            throw new BusinessException("Book not found with id: " + id);
        }
    }

    @Override
    public boolean existsById(Long id) {
        return bookRepository.existsById(id);
    }

    @Override
    public List<Book> getBooksByAuthor(Long authorId) {
        return bookRepository.findByAuthorId(authorId);
    }

    @Override
    public List<Book> getBooksByCategory(Long categoryId) {
        return bookRepository.findByCategoryId(categoryId);
    }

    @Override
    public List<Book> findBooksByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public List<Book> findBooksByTitleContaining(String title) {
        return bookRepository.findBooksByTitleContaining(title);
    }

    @Override
    public List<Book> findBooksByAuthor(String author) {
        return bookRepository.findAll(); // Simplified - would need proper implementation
    }

    @Override
    public List<Book> findBooksWithAvailableCopies() {
        return bookRepository.findBooksWithAvailableCopies();
    }

    private BookResponseDto toDto(Book book) {
        BookResponseDto dto = new BookResponseDto();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setIsbn(book.getIsbn());
        dto.setAuthorName(book.getAuthor().getName());
        dto.setAuthorLastname(book.getAuthor().getLastname());
        dto.setCategoryName(book.getCategory().getName());
        dto.setPublicationDate(book.getPublicationDate());
        dto.setTotalCopies(book.getTotalCopies());
        dto.setAvailableCopies(book.getAvailableCopies());
        return dto;
    }
}







