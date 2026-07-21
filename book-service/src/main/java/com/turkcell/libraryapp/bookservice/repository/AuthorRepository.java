package com.turkcell.libraryapp.bookservice.repository;

import com.turkcell.libraryapp.bookservice.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {
    
    @Query("SELECT a FROM Author a WHERE a.name = :name AND a.lastname = :lastname")
    Optional<Author> findByNameAndLastname(@Param("name") String name, @Param("lastname") String lastname);
    
    @Query("SELECT a FROM Author a WHERE SIZE(a.books) > :bookCount")
    List<Author> findAuthorsWithMoreThanBooks(@Param("bookCount") int bookCount);
    
    @Query("SELECT a FROM Author a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :name, '%')) OR LOWER(a.lastname) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Author> findAuthorsByNameContaining(@Param("name") String name);
}







