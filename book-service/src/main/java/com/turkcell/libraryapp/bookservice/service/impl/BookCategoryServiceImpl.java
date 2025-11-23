package com.turkcell.libraryapp.bookservice.service.impl;

import com.turkcell.libraryapp.bookservice.entity.BookCategory;
import com.turkcell.libraryapp.bookservice.exception.BusinessException;
import com.turkcell.libraryapp.bookservice.repository.BookCategoryRepository;
import com.turkcell.libraryapp.bookservice.service.BookCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookCategoryServiceImpl implements BookCategoryService {

    @Autowired
    private BookCategoryRepository categoryRepository;

    @Override
    public List<BookCategory> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Optional<BookCategory> getCategoryById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    public BookCategory createCategory(BookCategory category) {
        return categoryRepository.save(category);
    }

    @Override
    public BookCategory updateCategory(Long id, BookCategory categoryDetails) {
        BookCategory existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Category with id " + id + " not found"));
        existingCategory.setName(categoryDetails.getName());
        return categoryRepository.save(existingCategory);
    }

    @Override
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new BusinessException("Category with id " + id + " not found");
        }
        categoryRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return categoryRepository.existsById(id);
    }
}

