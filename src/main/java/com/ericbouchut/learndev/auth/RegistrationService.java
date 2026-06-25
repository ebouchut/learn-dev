package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.auth.dto.RegisterForm;
import com.ericbouchut.learndev.auth.exception.DuplicateEmailException;
import com.ericbouchut.learndev.auth.exception.DuplicateUsernameException;
import com.ericbouchut.learndev.role.entity.Role;
import com.ericbouchut.learndev.role.repository.RoleRepository;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
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
        if (users.existsByUsername(form.username())) {
            throw new DuplicateUsernameException(form.username());
        }
        if (users.existsByEmail(form.email())) {
            throw new DuplicateEmailException(form.email());
        }
        Role student = roles.findByRoleName("STUDENT")
                .orElseThrow(() -> new IllegalStateException("STUDENT role not seeded"));

        User user = new User();
        user.setUsername(form.username());
        user.setEmail(form.email());
        user.setPassword(encoder.encode(form.password()));
        user.getRoles().add(student);

        return users.save(user);
    }
}
