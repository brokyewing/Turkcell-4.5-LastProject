package com.turkcell.libraryapp.bookservice.service;

import com.turkcell.libraryapp.bookservice.entity.Author;
import java.util.List;
import java.util.Optional;

public interface AuthorService {
    List<Author> getAllAuthors();
    Optional<Author> getAuthorById(Long id);
    Author createAuthor(Author author);
    Author updateAuthor(Long id, Author authorDetails);
    void deleteAuthor(Long id);
    boolean existsById(Long id);
    List<Author> findAuthorsByNameContaining(String name);
    List<Author> findAuthorsWithMoreThanBooks(int bookCount);
}







