package com.turkcell.libraryapp.bookservice.service;

import com.turkcell.libraryapp.bookservice.entity.CopyBook;
import java.util.List;
import java.util.Optional;

public interface CopyBookService {
    List<CopyBook> getAllCopyBooks();
    Optional<CopyBook> getCopyBookById(Long id);
    CopyBook createCopyBook(CopyBook copyBook);
    void deleteCopyBook(Long id);
    boolean existsById(Long id);
    boolean isAvailable(Long id);
    List<CopyBook> getCopyBooksByBookId(Long bookId);
}



