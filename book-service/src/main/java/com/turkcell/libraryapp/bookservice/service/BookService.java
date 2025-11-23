package com.turkcell.libraryapp.bookservice.service;

import com.turkcell.libraryapp.bookservice.entity.Book;
import java.util.List;
import java.util.Optional;

public interface BookService {
    List<Book> getAllBooks();
    Optional<Book> getBookById(Long id);
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
}



