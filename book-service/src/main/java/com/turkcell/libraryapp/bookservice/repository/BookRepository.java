package com.turkcell.libraryapp.bookservice.repository;

import com.turkcell.libraryapp.bookservice.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * Stoğu atomik olarak 1 azaltır. Tek bir UPDATE cümlesi olduğu için "oku-sonra-yaz"
     * yarışı yoktur: iki eşzamanlı istek gelirse ikincisi ya 0 satır etkiler ya da
     * güncel değeri görür. Etkilenen satır 0 ise stok yok (ya da kitap yok).
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Book b SET b.availableCopies = b.availableCopies - 1 " +
           "WHERE b.id = :id AND b.availableCopies > 0")
    int decrementAvailableCopies(@Param("id") Long id);

    /**
     * İadede stoğu atomik olarak 1 artırır; stok hiçbir zaman totalCopies'i aşamaz.
     * `availableCopies < totalCopies` koşulu, iade telafisinin stoğu şişirmesini engeller.
     * Etkilenen satır 0 ise ya kitap yok ya da stok zaten dolu.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Book b SET b.availableCopies = b.availableCopies + 1 " +
           "WHERE b.id = :id AND b.availableCopies < b.totalCopies")
    int incrementAvailableCopies(@Param("id") Long id);
}







