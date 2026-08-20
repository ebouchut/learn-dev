package com.ericbouchut.learndev.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Form backing the "reset password" page.
 * Inbound DTO: Web Browser => Controller.
 *
 * @param token    the raw reset token, carried in a hidden field
 * @param password the new raw password (same policy as registration;
 *                 hashed before storage)
 */
public record ResetPasswordForm(
        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, max = 100)
        String password
) {
}
