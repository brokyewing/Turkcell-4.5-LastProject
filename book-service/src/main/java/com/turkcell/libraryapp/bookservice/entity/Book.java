package com.turkcell.libraryapp.bookservice.entity;

import com.turkcell.libraryapp.bookservice.enums.BookStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "books")
public class Book {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "title", nullable = false)
    private String title;
    
    @Column(name = "isbn", nullable = false, unique = true)
    private String isbn;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorid", nullable = false)
    private Author author;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoryid", nullable = false)
    private BookCategory category;
    
    @Column(name = "publicationdate")
    private LocalDate publicationDate;
    
    @Column(name = "total_copies", nullable = false)
    private Integer totalCopies = 0;
    
    @Column(name = "available_copies", nullable = false)
    private Integer availableCopies = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookStatus status = BookStatus.ACTIVE;
    
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CopyBook> copyBooks;
    
    public Book() {}
    
    public Book(String title, Author author, BookCategory category, LocalDate publicationDate) {
        this.title = title;
        this.author = author;
        this.category = category;
        this.publicationDate = publicationDate;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }
    public BookCategory getCategory() { return category; }
    public void setCategory(BookCategory category) { this.category = category; }
    public LocalDate getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDate publicationDate) { this.publicationDate = publicationDate; }
    public Integer getTotalCopies() { return totalCopies; }
    public void setTotalCopies(Integer totalCopies) { this.totalCopies = totalCopies; }
    public Integer getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }
    public BookStatus getStatus() { return status; }
    public void setStatus(BookStatus status) { this.status = status; }
    public List<CopyBook> getCopyBooks() { return copyBooks; }
    public void setCopyBooks(List<CopyBook> copyBooks) { this.copyBooks = copyBooks; }
}







