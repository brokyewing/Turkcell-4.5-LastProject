package com.turkcell.libraryapp.memberservice.security;

public class LoginResponse {
    private String token;
    private String studentNumber;
    private String name;

    public LoginResponse(String token, String studentNumber, String name) {
        this.token = token;
        this.studentNumber = studentNumber;
        this.name = name;
    }

    public String getToken() { return token; }
    public String getStudentNumber() { return studentNumber; }
    public String getName() { return name; }
}
