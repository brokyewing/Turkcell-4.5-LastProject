package com.turkcell.libraryapp.bookservice.controller;

import com.turkcell.libraryapp.bookservice.dto.response.CategoryResponseDto;
import com.turkcell.libraryapp.bookservice.entity.BookCategory;
import com.turkcell.libraryapp.bookservice.service.BookCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class BookCategoryController {

    private final BookCategoryService categoryService;

    @GetMapping
    public List<CategoryResponseDto> getAllCategories() {
        return categoryService.getAllCategories().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> getById(@PathVariable Long id) {
        return categoryService.getCategoryById(id)
                .map(c -> ResponseEntity.ok(mapToDto(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public CategoryResponseDto add(@RequestBody BookCategory category) {
        return mapToDto(categoryService.createCategory(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDto> update(@PathVariable Long id, @RequestBody BookCategory categoryDetails) {
        BookCategory updated = categoryService.updateCategory(id, categoryDetails);
        return ResponseEntity.ok(mapToDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok().build();
    }

    // Entity → DTO: sadece dışarıya açılacak alanlar. 'books' koleksiyonu dışarı çıkmaz.
    private CategoryResponseDto mapToDto(BookCategory category) {
        return new CategoryResponseDto(category.getId(), category.getName());
    }
}
