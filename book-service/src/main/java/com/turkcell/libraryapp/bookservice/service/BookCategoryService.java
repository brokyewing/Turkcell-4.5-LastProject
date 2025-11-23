package com.turkcell.libraryapp.bookservice.service;

import com.turkcell.libraryapp.bookservice.entity.BookCategory;
import java.util.List;
import java.util.Optional;

public interface BookCategoryService {
    List<BookCategory> getAllCategories();
    Optional<BookCategory> getCategoryById(Long id);
    BookCategory createCategory(BookCategory category);
    BookCategory updateCategory(Long id, BookCategory categoryDetails);
    void deleteCategory(Long id);
    boolean existsById(Long id);
}



