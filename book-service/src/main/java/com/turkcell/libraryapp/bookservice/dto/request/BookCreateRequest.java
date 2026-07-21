package com.turkcell.libraryapp.bookservice.dto.request;

import com.turkcell.libraryapp.bookservice.enums.BookStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class BookCreateRequest {
    @NotBlank(message = "Title is required")
    @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
    private String title;
    
    @NotBlank(message = "ISBN is required")
    @Pattern(regexp = "^[0-9]{13}$", message = "ISBN must be exactly 13 digits")
    private String isbn;
    
    @NotNull(message = "Author ID is required")
    private Long authorId;
    
    @NotNull(message = "Category ID is required")
    private Long categoryId;
    
    private LocalDate publicationDate;
    
    @NotNull(message = "Total copies is required")
    @Positive(message = "Total copies must be positive")
    private Integer totalCopies;
    
    @NotNull(message = "Available copies is required")
    @Positive(message = "Available copies must be positive")
    private Integer availableCopies;
    
    private BookStatus status = BookStatus.ACTIVE;
    
    // Getters and Setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public LocalDate getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDate publicationDate) { this.publicationDate = publicationDate; }
    public Integer getTotalCopies() { return totalCopies; }
    public void setTotalCopies(Integer totalCopies) { this.totalCopies = totalCopies; }
    public Integer getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }
    public BookStatus getStatus() { return status; }
    public void setStatus(BookStatus status) { this.status = status; }
}







