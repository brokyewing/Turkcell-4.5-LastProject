package com.turkcell.libraryapp.bookservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "copybooks")
public class CopyBook {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookid", nullable = false)
    private Book book;
    
    public CopyBook() {}
    
    public CopyBook(Book book) {
        this.book = book;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
}







