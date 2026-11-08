package com.turkcell.libraryapp.bookservice.service;

import com.turkcell.libraryapp.bookservice.dto.response.CopyBookStockDto;
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

    /** Fulfill kontrolü: kopyanın kitabı + kitabın müsait stok adedi. Boş → kopya yok. */
    Optional<CopyBookStockDto> getStockByCopyBookId(Long copyBookId);

    /** Kopyanın bağlı olduğu kitabın stoğunu 1 azaltır. false → stok yok. */
    boolean decrementAvailableByCopyBook(Long copyBookId);

    /** Kopyanın bağlı olduğu kitabın stoğunu 1 artırır (iade). false → stok zaten dolu. */
    boolean incrementAvailableByCopyBook(Long copyBookId);
}







