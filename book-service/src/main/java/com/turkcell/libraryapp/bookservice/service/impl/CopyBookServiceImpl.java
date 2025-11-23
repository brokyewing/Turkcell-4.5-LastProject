package com.turkcell.libraryapp.bookservice.service.impl;

import com.turkcell.libraryapp.bookservice.entity.CopyBook;
import com.turkcell.libraryapp.bookservice.exception.BusinessException;
import com.turkcell.libraryapp.bookservice.repository.CopyBookRepository;
import com.turkcell.libraryapp.bookservice.service.CopyBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CopyBookServiceImpl implements CopyBookService {

    @Autowired
    private CopyBookRepository copyBookRepository;

    @Override
    public List<CopyBook> getAllCopyBooks() {
        return copyBookRepository.findAll();
    }

    @Override
    public Optional<CopyBook> getCopyBookById(Long id) {
        return copyBookRepository.findById(id);
    }

    @Override
    public CopyBook createCopyBook(CopyBook copyBook) {
        return copyBookRepository.save(copyBook);
    }

    @Override
    public void deleteCopyBook(Long id) {
        if (!copyBookRepository.existsById(id)) {
            throw new BusinessException("CopyBook not found with id: " + id);
        }
        copyBookRepository.deleteById(id);
    }

    @Override
    public boolean existsById(Long id) {
        return copyBookRepository.existsById(id);
    }

    @Override
    public boolean isAvailable(Long id) {
        Optional<CopyBook> copyBook = copyBookRepository.findById(id);
        return copyBook.isPresent(); // Simplified - would need to check if not loaned
    }

    @Override
    public List<CopyBook> getCopyBooksByBookId(Long bookId) {
        return copyBookRepository.findByBookId(bookId);
    }
}

