package com.turkcell.libraryapp.memberservice.security;

public class LoginResponse {
    private String token;
    private String studentNumber;
    private String name;
    private String role;

    public LoginResponse(String token, String studentNumber, String name, String role) {
        this.token = token;
        this.studentNumber = studentNumber;
        this.name = name;
        this.role = role;
    }

    public String getToken() { return token; }
    public String getStudentNumber() { return studentNumber; }
    public String getName() { return name; }
    public String getRole() { return role; }
}
