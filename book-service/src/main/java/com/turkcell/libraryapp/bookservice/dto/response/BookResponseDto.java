package com.turkcell.libraryapp.bookservice.dto.response;

import java.time.LocalDate;

public class BookResponseDto {
    private Long id;
    private String title;
    private String isbn;
    private String authorName;
    private String authorLastname;
    private String categoryName;
    private LocalDate publicationDate;
    private Integer totalCopies;
    private Integer availableCopies;
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorLastname() { return authorLastname; }
    public void setAuthorLastname(String authorLastname) { this.authorLastname = authorLastname; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public LocalDate getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDate publicationDate) { this.publicationDate = publicationDate; }
    public Integer getTotalCopies() { return totalCopies; }
    public void setTotalCopies(Integer totalCopies) { this.totalCopies = totalCopies; }
    public Integer getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }
}



