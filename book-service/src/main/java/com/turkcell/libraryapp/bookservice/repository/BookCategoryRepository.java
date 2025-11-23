package com.turkcell.libraryapp.bookservice.repository;

import com.turkcell.libraryapp.bookservice.entity.BookCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookCategoryRepository extends JpaRepository<BookCategory, Long> {
}



