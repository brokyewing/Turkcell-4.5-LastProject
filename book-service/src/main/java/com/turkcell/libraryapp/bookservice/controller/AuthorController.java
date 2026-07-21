package com.turkcell.libraryapp.bookservice.controller;

import com.turkcell.libraryapp.bookservice.dto.request.AuthorRequestDto;
import com.turkcell.libraryapp.bookservice.dto.response.AuthorResponseDto;
import com.turkcell.libraryapp.bookservice.entity.Author;
import com.turkcell.libraryapp.bookservice.service.AuthorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/authors")
@RequiredArgsConstructor
public class AuthorController {

    private final AuthorService authorService;

    @GetMapping
    public List<AuthorResponseDto> getAllAuthors() {
        List<Author> authors = authorService.getAllAuthors();
        return authors.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuthorResponseDto> getById(@PathVariable Long id) {
        Optional<Author> author = authorService.getAuthorById(id);
        return author.map(a -> ResponseEntity.ok(mapToDto(a)))
                    .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<AuthorResponseDto> add(@RequestBody AuthorRequestDto authorRequest) {
        Author author = new Author();
        author.setName(authorRequest.getName());
        author.setLastname(authorRequest.getLastname());
        Author createdAuthor = authorService.createAuthor(author);
        AuthorResponseDto response = mapToDto(createdAuthor);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AuthorResponseDto> update(@PathVariable Long id, @RequestBody AuthorRequestDto authorRequest) {
        Author author = new Author();
        author.setName(authorRequest.getName());
        author.setLastname(authorRequest.getLastname());
        Author updatedAuthor = authorService.updateAuthor(id, author);
        AuthorResponseDto response = mapToDto(updatedAuthor);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        authorService.deleteAuthor(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/search")
    public List<AuthorResponseDto> searchAuthors(@RequestParam String name) {
        List<Author> authors = authorService.findAuthorsByNameContaining(name);
        return authors.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    @GetMapping("/prolific")
    public List<AuthorResponseDto> getProlificAuthors(@RequestParam int bookCount) {
        List<Author> authors = authorService.findAuthorsWithMoreThanBooks(bookCount);
        return authors.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }
    
    private AuthorResponseDto mapToDto(Author author) {
        AuthorResponseDto dto = new AuthorResponseDto();
        dto.setId(author.getId());
        dto.setName(author.getName());
        dto.setLastname(author.getLastname());
        return dto;
    }
}







