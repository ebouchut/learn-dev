package com.ericbouchut.learndev.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code RegisterForm}  holds the data submitted by a browser HTML form for user registration.
 * It is validated with Bean Validation.
 * The constraints are enforced when a controller binds it with {@code @Valid},
 * so invalid input is rejected before it reaches the service.
 * It is an inbound Data Transfer Object (DTO): Web Browser => Controller.
 *
 * @param username chosen username (3 to 50 characters)
 * @param email    email address (valid format, up to 255 characters)
 * @param password raw password (8 to 100 characters) (MUST be hashed before storage)
 */
public record RegisterForm(
        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @NotBlank
        @Email @Size(max = 255)
        String email,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}
