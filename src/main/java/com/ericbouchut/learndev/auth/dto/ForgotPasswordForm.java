package com.ericbouchut.learndev.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form backing the "forgot password" request page.
 * Inbound DTO: Web Browser => Controller.
 *
 * @param email the address to send the reset link to
 */
public record ForgotPasswordForm(
        @NotBlank
        @Email @Size(max = 255)
        String email
) {
}
