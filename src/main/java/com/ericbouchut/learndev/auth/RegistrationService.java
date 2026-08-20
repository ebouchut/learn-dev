package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.auth.dto.RegisterForm;
import com.ericbouchut.learndev.auth.exception.DuplicateEmailException;
import com.ericbouchut.learndev.auth.exception.DuplicateUsernameException;
import com.ericbouchut.learndev.role.entity.Role;
import com.ericbouchut.learndev.role.repository.RoleRepository;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates new user accounts: enforces unique username and email, hashes the
 * password, and assigns the default {@code STUDENT} role.
 */
@Service
public class RegistrationService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public RegistrationService(UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    /**
     * Registers a new account with the default {@code STUDENT} role. The password
     * is hashed before being stored; the raw password is never persisted.
     *
     * @param form the validated registration form
     * @return the saved user, including its generated id
     * @throws DuplicateUsernameException if the username is already taken
     * @throws DuplicateEmailException    if the email is already registered
     */
    @Transactional
    public User register(RegisterForm form) {
        return register(form, "STUDENT");
    }

    /**
     * Registers a new account with the given seeded role (self-registration
     * uses {@code STUDENT}; the admin area creates {@code INSTRUCTOR}
     * accounts). Same uniqueness and hashing rules as {@link #register}.
     *
     * @param form     the validated registration form
     * @param roleName the seeded role to assign
     * @return the saved user, including its generated id
     * @throws DuplicateUsernameException if the username is already taken
     * @throws DuplicateEmailException    if the email is already registered
     */
    @Transactional
    public User register(RegisterForm form, String roleName) {
        if (users.existsByUsername(form.username())) {
            throw new DuplicateUsernameException(form.username());
        }
        if (users.existsByEmail(form.email())) {
            throw new DuplicateEmailException(form.email());
        }
        Role role = roles.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " role not seeded"));

        User user = new User();
        user.setUsername(form.username());
        user.setEmail(form.email());
        user.setPassword(encoder.encode(form.password()));
        user.getRoles().add(role);

        // The existsBy* pre-checks above race under concurrency: two requests can
        // both pass them, and the loser hits the users_username_key/users_email_key
        // UNIQUE constraint. Flush inside this method (saveAndFlush, not save) so
        // the violation is catchable here, and map it back to the domain exception
        // by constraint name: after a failed statement PostgreSQL aborts the
        // transaction, so re-querying existsBy* in the catch would also fail.
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            String constraint = constraintName(e);
            if ("users_username_key".equalsIgnoreCase(constraint)) {
                throw new DuplicateUsernameException(form.username());
            }
            if ("users_email_key".equalsIgnoreCase(constraint)) {
                throw new DuplicateEmailException(form.email());
            }
            throw e;
        }
    }

    /**
     * Extracts the database constraint name from a data-integrity failure, or
     * {@code null} when the cause chain has no {@link ConstraintViolationException}.
     * @param e a data integrity violation exception
     * @return the name of violated constraint name if any or null otherwise.
     */
    private static String constraintName(DataIntegrityViolationException e) {
        for (Throwable cause = e.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }
}
