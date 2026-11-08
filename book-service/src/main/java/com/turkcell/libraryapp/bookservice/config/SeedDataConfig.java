package com.turkcell.libraryapp.bookservice.config;

import com.turkcell.libraryapp.bookservice.entity.Author;
import com.turkcell.libraryapp.bookservice.entity.Book;
import com.turkcell.libraryapp.bookservice.entity.BookCategory;
import com.turkcell.libraryapp.bookservice.entity.CopyBook;
import com.turkcell.libraryapp.bookservice.enums.BookStatus;
import com.turkcell.libraryapp.bookservice.repository.AuthorRepository;
import com.turkcell.libraryapp.bookservice.repository.BookCategoryRepository;
import com.turkcell.libraryapp.bookservice.repository.BookRepository;
import com.turkcell.libraryapp.bookservice.repository.CopyBookRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.LocalDate;

/**
 * Boş veritabanına demo katalog ekler (2 yazar, 3 kategori, 3 kitap, her kitaba 2 kopya).
 * Idempotent: books tablosunda kayıt varsa hiçbir şey yapmaz.
 */
@Configuration
@Profile("!test")
public class SeedDataConfig {

    @Bean
    public CommandLineRunner seedBookData(AuthorRepository authorRepository,
                                          BookCategoryRepository categoryRepository,
                                          BookRepository bookRepository,
                                          CopyBookRepository copyBookRepository) {
        return args -> {
            if (bookRepository.count() > 0) {
                return;
            }

            Author orwell = authorRepository.save(new Author("George", "Orwell"));
            Author sabahattin = authorRepository.save(new Author("Sabahattin", "Ali"));

            BookCategory roman = categoryRepository.save(new BookCategory("Roman"));
            BookCategory bilimKurgu = categoryRepository.save(new BookCategory("Bilim Kurgu"));
            BookCategory oyku = categoryRepository.save(new BookCategory("Öykü"));

            seedBook(bookRepository, copyBookRepository,
                    "1984", "9789750718533", orwell, bilimKurgu, LocalDate.of(1949, 6, 8));
            seedBook(bookRepository, copyBookRepository,
                    "Hayvan Çiftliği", "9789750718540", orwell, roman, LocalDate.of(1945, 8, 17));
            seedBook(bookRepository, copyBookRepository,
                    "Kürk Mantolu Madonna", "9789753638029", sabahattin, oyku, LocalDate.of(1943, 1, 1));
        };
    }

    private void seedBook(BookRepository bookRepository,
                          CopyBookRepository copyBookRepository,
                          String title, String isbn, Author author, BookCategory category,
                          LocalDate publicationDate) {
        Book book = new Book(title, author, category, publicationDate);
        book.setIsbn(isbn);
        book.setTotalCopies(2);
        book.setAvailableCopies(2);
        book.setStatus(BookStatus.ACTIVE);
        Book saved = bookRepository.save(book);

        // totalCopies=2 → iki fiziksel kopya.
        copyBookRepository.save(new CopyBook(saved));
        copyBookRepository.save(new CopyBook(saved));
    }
}
