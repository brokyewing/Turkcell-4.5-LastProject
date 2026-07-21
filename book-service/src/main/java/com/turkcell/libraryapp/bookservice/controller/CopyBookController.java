package com.turkcell.libraryapp.bookservice.controller;

import com.turkcell.libraryapp.bookservice.dto.response.CopyBookResponseDto;
import com.turkcell.libraryapp.bookservice.entity.CopyBook;
import com.turkcell.libraryapp.bookservice.service.CopyBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/copybooks")
@RequiredArgsConstructor
public class CopyBookController {

    private final CopyBookService copyBookService;

    @GetMapping
    public List<CopyBookResponseDto> getAllCopyBooks() {
        return copyBookService.getAllCopyBooks().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CopyBookResponseDto> getCopyBookById(@PathVariable Long id) {
        return copyBookService.getCopyBookById(id)
                .map(c -> ResponseEntity.ok(mapToDto(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/exists")
    public Boolean existsById(@PathVariable Long id) {
        return copyBookService.existsById(id);
    }

    @GetMapping("/{id}/available")
    public Boolean isAvailable(@PathVariable Long id) {
        return copyBookService.isAvailable(id);
    }

    @PostMapping
    public ResponseEntity<CopyBookResponseDto> createCopyBook(@RequestBody CopyBook copyBook) {
        CopyBook created = copyBookService.createCopyBook(copyBook);
        return ResponseEntity.ok(mapToDto(created));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCopyBook(@PathVariable Long id) {
        copyBookService.deleteCopyBook(id);
        return ResponseEntity.ok().build();
    }

    // Entity → DTO: iç içe Book entity'si yerine düz bookId/bookTitle.
    private CopyBookResponseDto mapToDto(CopyBook copyBook) {
        var book = copyBook.getBook();
        return new CopyBookResponseDto(
                copyBook.getId(),
                book != null ? book.getId() : null,
                book != null ? book.getTitle() : null);
    }
}
