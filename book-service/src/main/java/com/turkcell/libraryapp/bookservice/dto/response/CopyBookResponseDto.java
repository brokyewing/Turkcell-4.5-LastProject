package com.turkcell.libraryapp.bookservice.dto.response;

// Fiziksel kitap kopyasının dış temsili. İç içe Book entity'si yerine
// sadece ihtiyaç duyulan düz alanlar (bookId, bookTitle) — döngü riski yok.
public class CopyBookResponseDto {
    private Long id;
    private Long bookId;
    private String bookTitle;

    public CopyBookResponseDto() {}

    public CopyBookResponseDto(Long id, Long bookId, String bookTitle) {
        this.id = id;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
}
