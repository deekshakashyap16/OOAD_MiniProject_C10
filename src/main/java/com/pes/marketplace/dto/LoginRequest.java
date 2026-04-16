package com.pes.marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for the login form.
 * SOLID – SRP: Only carries login credentials.
 */
public class LoginRequest {

    @Email @NotBlank
    private String email;

    @NotBlank
    private String password;

    public String getEmail()                    { return email; }
    public void setEmail(String email)          { this.email = email; }
    public String getPassword()                 { return password; }
    public void setPassword(String password)    { this.password = password; }
}
