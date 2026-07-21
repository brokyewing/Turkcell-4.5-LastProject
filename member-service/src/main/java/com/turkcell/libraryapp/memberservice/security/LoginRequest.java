package com.turkcell.libraryapp.memberservice.security;

public class LoginRequest {
    private String studentNumber;
    private String password;

    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
