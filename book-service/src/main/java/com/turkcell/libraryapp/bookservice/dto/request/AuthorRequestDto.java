package com.turkcell.libraryapp.bookservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthorRequestDto {
    @NotBlank(message = "Author name is required")
    @Size(min = 2, max = 50, message = "Author name must be between 2 and 50 characters")
    private String name;
    
    @NotBlank(message = "Author lastname is required")
    @Size(min = 2, max = 50, message = "Author lastname must be between 2 and 50 characters")
    private String lastname;
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }
}



