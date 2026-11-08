package com.turkcell.libraryapp.bookservice.dto.response;

// Rezervasyon fulfill kontrolü için: kopyanın bağlı olduğu kitap + o kitabın
// o anki müsait stok adedi. Müsaitlik kitap seviyesinde olduğu için adet gerekir.
public class CopyBookStockDto {
    private Long bookId;
    private Integer availableCopies;

    public CopyBookStockDto() {}

    public CopyBookStockDto(Long bookId, Integer availableCopies) {
        this.bookId = bookId;
        this.availableCopies = availableCopies;
    }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public Integer getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(Integer availableCopies) { this.availableCopies = availableCopies; }
}
