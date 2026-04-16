package com.pes.marketplace.dto;

import com.pes.marketplace.model.Role;
import jakarta.validation.constraints.*;

/**
 * DTO for the registration form.
 *
 * SOLID – SRP : Only carries registration data; no persistence logic.
 * GRASP – Low Coupling : Decouples the HTTP layer from the User entity.
 */
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    @Email(message = "Valid email required")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    @NotNull(message = "Select a role")
    private Role role;

    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }

    // Getters & Setters
    public String getName()                         { return name; }
    public void setName(String name)                { this.name = name; }
    public String getEmail()                        { return email; }
    public void setEmail(String email)              { this.email = email; }
    public String getPassword()                     { return password; }
    public void setPassword(String password)        { this.password = password; }
    public String getConfirmPassword()              { return confirmPassword; }
    public void setConfirmPassword(String c)        { this.confirmPassword = c; }
    public Role getRole()                           { return role; }
    public void setRole(Role role)                  { this.role = role; }
}
