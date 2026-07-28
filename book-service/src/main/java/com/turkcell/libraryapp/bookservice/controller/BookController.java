package com.turkcell.libraryapp.bookservice.controller;

import com.turkcell.libraryapp.bookservice.dto.request.BookCreateRequest;
import com.turkcell.libraryapp.bookservice.dto.response.BookResponseDto;
import com.turkcell.libraryapp.bookservice.entity.Book;
import com.turkcell.libraryapp.bookservice.exception.BusinessException;
import com.turkcell.libraryapp.bookservice.service.AuthorService;
import com.turkcell.libraryapp.bookservice.service.BookCategoryService;
import com.turkcell.libraryapp.bookservice.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Tag(name = "Book Management", description = "Kitap CRUD operasyonları ve arama")
public class BookController {

    private final BookService bookService;
    private final AuthorService authorService;
    private final BookCategoryService categoryService;

    @Operation(summary = "Yeni kitap ekle", description = "ISBN, başlık, yazar ve kategori bilgisi ile yeni kitap oluşturur")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kitap başarıyla oluşturuldu"),
            @ApiResponse(responseCode = "400", description = "Geçersiz istek veya eksik alan"),
            @ApiResponse(responseCode = "404", description = "Yazar veya kategori bulunamadı")
    })
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

    @Operation(summary = "Kitap kopya sayısını güncelle", description = "Belirli bir kitabın toplam ve mevcut kopya sayısını artırır/azaltır")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kopya sayısı güncellendi"),
            @ApiResponse(responseCode = "400", description = "Negatif kopya sayısı veya geçersiz delta"),
            @ApiResponse(responseCode = "404", description = "Kitap bulunamadı")
    })
    @PatchMapping("/{id}/copies")
    public ResponseEntity<BookResponseDto> updateBookCopies(
            @Parameter(description = "Kitap ID") @PathVariable Long id,
            @Parameter(description = "Kopya sayısı değişimi (+/-)") @RequestParam String delta) {

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

    @Operation(summary = "Kitapları listele", description = "Filtreleme veya sayfalama ile kitap listesi")
    @ApiResponse(responseCode = "200", description = "Kitap listesi veya sayfa")
    @GetMapping
    public ResponseEntity<?> getBooks(
            @Parameter(description = "ISBN numarası ile ara") @RequestParam(required = false) String isbn,
            @Parameter(description = "Başlık içinde ara") @RequestParam(required = false) String title,
            @Parameter(description = "Yazar adı ile ara") @RequestParam(required = false) String author,
            @Parameter(description = "Sadece müsait kitaplar") @RequestParam(required = false) Boolean available,
            @Parameter(description = "Sayfalama") @PageableDefault(size = 20, sort = "title") Pageable pageable) {

        if (isbn == null && title == null && author == null && available == null) {
            Page<BookResponseDto> responses = bookService.getAllBooks(pageable)
                    .map(this::mapToDto);
            return ResponseEntity.ok(responses);
        }

        List<Book> books;
        if (isbn != null) {
            books = bookService.findBooksByIsbn(isbn);
        } else if (title != null) {
            books = bookService.findBooksByTitleContaining(title);
        } else if (author != null) {
            books = bookService.findBooksByAuthor(author);
        } else {
            books = bookService.findBooksWithAvailableCopies();
        }

        List<BookResponseDto> responses = books.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "Kitap detayı getir", description = "ID'ye göre tek bir kitabın detaylarını döndürür (Redis cache'li)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Kitap bulundu"),
            @ApiResponse(responseCode = "404", description = "Kitap bulunamadı")
    })
    @GetMapping("/{id}")
    public ResponseEntity<BookResponseDto> getBookById(
            @Parameter(description = "Kitap ID") @PathVariable Long id) {
        return ResponseEntity.ok(bookService.getBookDtoById(id));
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