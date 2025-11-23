package com.turkcell.libraryapp.bookservice.service.impl;

import com.turkcell.libraryapp.bookservice.entity.Book;
import com.turkcell.libraryapp.bookservice.repository.BookRepository;
import com.turkcell.libraryapp.bookservice.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookRepository bookRepository;

    @Override
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    @Override
    public Book createBook(Book book) {
        return bookRepository.save(book);
    }

    @Override
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
        throw new RuntimeException("Book not found with id: " + id);
    }

    @Override
    public void deleteBook(Long id) {
        if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id);
        } else {
            throw new RuntimeException("Book not found with id: " + id);
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
}



