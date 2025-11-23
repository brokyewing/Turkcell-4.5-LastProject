package com.turkcell.libraryapp.bookservice.controller;

import com.turkcell.libraryapp.bookservice.entity.CopyBook;
import com.turkcell.libraryapp.bookservice.service.CopyBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/copybooks")
public class CopyBookController {

    @Autowired
    private CopyBookService copyBookService;

    @GetMapping
    public List<CopyBook> getAllCopyBooks() {
        return copyBookService.getAllCopyBooks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CopyBook> getCopyBookById(@PathVariable Long id) {
        Optional<CopyBook> copyBook = copyBookService.getCopyBookById(id);
        return copyBook.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
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
    public ResponseEntity<CopyBook> createCopyBook(@RequestBody CopyBook copyBook) {
        CopyBook created = copyBookService.createCopyBook(copyBook);
        return ResponseEntity.ok(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCopyBook(@PathVariable Long id) {
        copyBookService.deleteCopyBook(id);
        return ResponseEntity.ok().build();
    }
}



