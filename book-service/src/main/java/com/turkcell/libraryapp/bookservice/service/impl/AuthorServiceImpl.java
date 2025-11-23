package com.turkcell.libraryapp.bookservice.service.impl;

import com.turkcell.libraryapp.bookservice.entity.Author;
import com.turkcell.libraryapp.bookservice.exception.BusinessException;
import com.turkcell.libraryapp.bookservice.repository.AuthorRepository;
import com.turkcell.libraryapp.bookservice.service.AuthorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthorServiceImpl implements AuthorService {

    @Autowired
    private AuthorRepository authorRepository;

    @Override
    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    @Override
    public Optional<Author> getAuthorById(Long id) {
        return authorRepository.findById(id);
    }

    @Override
    public Author createAuthor(Author author) {
        Optional<Author> existing = authorRepository.findByNameAndLastname(author.getName(), author.getLastname());
        if (existing.isPresent()) {
            throw new BusinessException("Author with name " + author.getName() + " " + author.getLastname() + " already exists");
        }
        return authorRepository.save(author);
    }

    @Override
    public Author updateAuthor(Long id, Author authorDetails) {
        Author existingAuthor = authorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Author with id " + id + " not found"));
        
        if (!existingAuthor.getName().equals(authorDetails.getName()) || 
            !existingAuthor.getLastname().equals(authorDetails.getLastname())) {
            Optional<Author> existing = authorRepository.findByNameAndLastname(authorDetails.getName(), authorDetails.getLastname());
            if (existing.isPresent()) {
                throw new BusinessException("Author with name " + authorDetails.getName() + " " + authorDetails.getLastname() + " already exists");
            }
        }
        
        existingAuthor.setName(authorDetails.getName());
        existingAuthor.setLastname(authorDetails.getLastname());
        return authorRepository.save(existingAuthor);
    }

    @Override
    public void deleteAuthor(Long id) {
        if (!authorRepository.existsById(id)) {
            throw new BusinessException("Author with id " + id + " not found");
        }
        authorRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return authorRepository.existsById(id);
    }

    @Override
    public List<Author> findAuthorsByNameContaining(String name) {
        return authorRepository.findAuthorsByNameContaining(name);
    }

    @Override
    public List<Author> findAuthorsWithMoreThanBooks(int bookCount) {
        return authorRepository.findAuthorsWithMoreThanBooks(bookCount);
    }
}



