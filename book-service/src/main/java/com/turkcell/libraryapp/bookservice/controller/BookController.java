package com.turkcell.libraryapp.bookservice.controller;

import com.turkcell.libraryapp.bookservice.dto.request.BookCreateRequest;
import com.turkcell.libraryapp.bookservice.dto.response.BookResponseDto;
import com.turkcell.libraryapp.bookservice.entity.Book;
import com.turkcell.libraryapp.bookservice.exception.BusinessException;
import com.turkcell.libraryapp.bookservice.service.AuthorService;
import com.turkcell.libraryapp.bookservice.service.BookCategoryService;
import com.turkcell.libraryapp.bookservice.service.BookService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
public class BookController {

    @Autowired
    private BookService bookService;
    
    @Autowired
    private AuthorService authorService;
    
    @Autowired
    private BookCategoryService categoryService;

    @PostMapping
    public ResponseEntity<BookResponseDto> createBook(@Valid @RequestBody BookCreateRequest request) {
        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setIsbn(request.getIsbn());
        book.setPublicationDate(request.getPublicationDate());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getAvailableCopies());
        book.setStatus(request.getStatus());
        
        book.setAuthor(authorService.getAuthorById(request.getAuthorId())
                .orElseThrow(() -> new BusinessException("Author not found with ID: " + request.getAuthorId())));
        book.setCategory(categoryService.getCategoryById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException("Category not found with ID: " + request.getCategoryId())));
        
        Book createdBook = bookService.createBook(book);
        BookResponseDto response = mapToDto(createdBook);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/copies")
    public ResponseEntity<BookResponseDto> updateBookCopies(
            @PathVariable Long id, 
            @RequestParam String delta) {
        
        Book book = bookService.getBookById(id)
                .orElseThrow(() -> new BusinessException("Book not found with id: " + id));
        
        int deltaValue = Integer.parseInt(delta);
        int newTotalCopies = book.getTotalCopies() + deltaValue;
        int newAvailableCopies = book.getAvailableCopies() + deltaValue;
        
        if (newTotalCopies < 0 || newAvailableCopies < 0) {
            return ResponseEntity.badRequest().build();
        }
        
        book.setTotalCopies(newTotalCopies);
        book.setAvailableCopies(newAvailableCopies);
        
        Book updatedBook = bookService.updateBook(id, book);
        BookResponseDto response = mapToDto(updatedBook);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BookResponseDto>> getBooks(
            @RequestParam(required = false) String isbn,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) Boolean available) {
        
        List<Book> books;
        if (isbn != null) {
            books = bookService.findBooksByIsbn(isbn);
        } else if (title != null) {
            books = bookService.findBooksByTitleContaining(title);
        } else if (author != null) {
            books = bookService.findBooksByAuthor(author);
        } else if (available != null && available) {
            books = bookService.findBooksWithAvailableCopies();
        } else {
            books = bookService.getAllBooks();
        }
        
        List<BookResponseDto> responses = books.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDto> getBookById(@PathVariable Long id) {
        Optional<Book> book = bookService.getBookById(id);
        return book.map(b -> ResponseEntity.ok(mapToDto(b)))
                  .orElse(ResponseEntity.notFound().build());
    }
    
    private BookResponseDto mapToDto(Book book) {
        BookResponseDto dto = new BookResponseDto();
        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setIsbn(book.getIsbn());
        dto.setAuthorName(book.getAuthor().getName());
        dto.setAuthorLastname(book.getAuthor().getLastname());
        dto.setCategoryName(book.getCategory().getName());
        dto.setPublicationDate(book.getPublicationDate());
        dto.setTotalCopies(book.getTotalCopies());
        dto.setAvailableCopies(book.getAvailableCopies());
        return dto;
    }
}



