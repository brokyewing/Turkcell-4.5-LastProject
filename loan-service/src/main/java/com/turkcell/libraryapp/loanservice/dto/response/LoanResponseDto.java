package com.turkcell.libraryapp.loanservice.dto.response;

import java.time.LocalDateTime;

public class LoanResponseDto {
    private Long id;
    private Long studentId;
    private Long copyBookId;
    private String studentName;
    private String studentLastname;
    private String bookTitle;
    private LocalDateTime loanDate;
    private LocalDateTime dueDate;
    private LocalDateTime returnDate;
    private Boolean isReturned;
    
    public LoanResponseDto() {}
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCopyBookId() { return copyBookId; }
    public void setCopyBookId(Long copyBookId) { this.copyBookId = copyBookId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentLastname() { return studentLastname; }
    public void setStudentLastname(String studentLastname) { this.studentLastname = studentLastname; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public LocalDateTime getLoanDate() { return loanDate; }
    public void setLoanDate(LocalDateTime loanDate) { this.loanDate = loanDate; }
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDateTime returnDate) { this.returnDate = returnDate; }
    public Boolean getIsReturned() { return isReturned; }
    public void setIsReturned(Boolean isReturned) { this.isReturned = isReturned; }
}







