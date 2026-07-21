package com.turkcell.libraryapp.bookservice.repository;

import com.turkcell.libraryapp.bookservice.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByAuthorId(Long authorId);
    List<Book> findByCategoryId(Long categoryId);
    
    @Query("SELECT b FROM Book b WHERE b.title = :title AND b.author.id = :authorId")
    Optional<Book> findByTitleAndAuthorId(@Param("title") String title, @Param("authorId") Long authorId);
    
    @Query("SELECT b FROM Book b WHERE b.publicationDate > :date ORDER BY b.publicationDate DESC")
    List<Book> findBooksPublishedAfter(@Param("date") LocalDate date);
    
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))")
    List<Book> findBooksByTitleContaining(@Param("title") String title);
    
    @Query("SELECT b FROM Book b WHERE b.availableCopies > 0")
    List<Book> findBooksWithAvailableCopies();
    
    Optional<Book> findByIsbn(String isbn);
}







