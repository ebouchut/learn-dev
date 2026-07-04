package com.ericbouchut.learndev.auth.exception;

/**
 * Thrown when registration is attempted with an email that is already registered.
 */
public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("Email already registered: " + email);
    }
}
