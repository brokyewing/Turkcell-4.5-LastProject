package com.turkcell.libraryapp.reservationservice.dto.response;

// book-service'in /api/copybooks/{id}/stock yanıtı: kopyanın kitabı + kitabın
// müsait stok adedi. Müsaitlik kitap seviyesinde olduğu için adet taşınır.
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
