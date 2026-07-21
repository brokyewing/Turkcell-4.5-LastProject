package com.turkcell.libraryapp.bookservice.repository;

import com.turkcell.libraryapp.bookservice.entity.Author;
import com.turkcell.libraryapp.bookservice.entity.Book;
import com.turkcell.libraryapp.bookservice.entity.BookCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testcontainers ile GERÇEK PostgreSQL'e karşı entegrasyon testi.
 *
 * Mockito "sahte DB" verir; bu ise Docker'da gerçek bir Postgres başlatır,
 * repository sorgularını ve JPA eşlemelerini gerçek veritabanında doğrular.
 * "Birimlerimi test ettim" ile "sistemin gerçekten çalıştığını kanıtladım" arasındaki fark.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class BookRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14");

    // Testcontainers'ın açtığı kabın adresini Spring'e bağla (rastgele port).
    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.cache.type", () -> "none"); // testte Redis gerekmesin
    }

    @Autowired BookRepository bookRepository;
    @Autowired AuthorRepository authorRepository;
    @Autowired BookCategoryRepository categoryRepository;

    @Test
    void savesBookAndFindsItByIsbn() {
        Author author = authorRepository.save(new Author("George", "Orwell"));
        BookCategory category = categoryRepository.save(new BookCategory("Roman"));

        Book book = new Book();
        book.setTitle("1984");
        book.setIsbn("9780451524935");
        book.setAuthor(author);
        book.setCategory(category);
        book.setTotalCopies(5);
        book.setAvailableCopies(5);
        bookRepository.save(book);

        Optional<Book> found = bookRepository.findByIsbn("9780451524935");
        assertTrue(found.isPresent());
        assertEquals("1984", found.get().getTitle());
        // İlişki gerçek DB'de doğru kuruldu mu?
        assertEquals("Orwell", found.get().getAuthor().getLastname());
        assertEquals("Roman", found.get().getCategory().getName());
    }

    @Test
    void findByIsbn_whenMissing_returnsEmpty() {
        assertTrue(bookRepository.findByIsbn("0000000000000").isEmpty());
    }
}
