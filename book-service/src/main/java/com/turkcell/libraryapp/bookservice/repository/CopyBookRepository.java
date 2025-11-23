package com.turkcell.libraryapp.bookservice.repository;

import com.turkcell.libraryapp.bookservice.entity.CopyBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CopyBookRepository extends JpaRepository<CopyBook, Long> {
    List<CopyBook> findByBookId(Long bookId);
}

