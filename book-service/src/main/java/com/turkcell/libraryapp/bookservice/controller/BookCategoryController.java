package com.turkcell.libraryapp.bookservice.controller;

import com.turkcell.libraryapp.bookservice.entity.BookCategory;
import com.turkcell.libraryapp.bookservice.service.BookCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
public class BookCategoryController {

    @Autowired
    private BookCategoryService categoryService;

    @GetMapping
    public List<BookCategory> getAllCategories() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookCategory> getById(@PathVariable Long id) {
        Optional<BookCategory> category = categoryService.getCategoryById(id);
        return category.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public BookCategory add(@RequestBody BookCategory category) {
        return categoryService.createCategory(category);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookCategory> update(@PathVariable Long id, @RequestBody BookCategory categoryDetails) {
        BookCategory updatedCategory = categoryService.updateCategory(id, categoryDetails);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }
}



