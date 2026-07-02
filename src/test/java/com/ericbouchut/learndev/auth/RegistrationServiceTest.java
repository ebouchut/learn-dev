package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.auth.dto.RegisterForm;
import com.ericbouchut.learndev.auth.exception.DuplicateEmailException;
import com.ericbouchut.learndev.auth.exception.DuplicateUsernameException;
import com.ericbouchut.learndev.role.entity.Role;
import com.ericbouchut.learndev.role.repository.RoleRepository;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegistrationServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RoleRepository roles = mock(RoleRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final RegistrationService service = new RegistrationService(users, roles, encoder);

    @Test
    void hashes_the_password_and_assigns_the_STUDENT_role() {
        // Arrange (Given)
        stubHappyPath();

        // Act (When)
        User created = service.register(new RegisterForm("lea", "lea@example.com", "secret12"));

        // Assert (Then)
        assertThat(created.getPassword()).isEqualTo("HASHED");                 // hashed, not raw
        assertThat(created.getRoles()).extracting(Role::getRoleName).containsExactly("STUDENT");
        verify(users).saveAndFlush(any(User.class));
    }

    @Test
    void rejects_a_duplicate_email() {
        when(users.existsByUsername("lea")).thenReturn(false);
        when(users.existsByEmail("lea@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterForm("lea", "lea@example.com", "secret12")))
                .isInstanceOf(DuplicateEmailException.class);
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void translates_a_username_constraint_violation_raced_past_the_pre_checks() {
        // Arrange (Given): pre-checks pass, but a concurrent insert wins the race
        // and the INSERT hits the username UNIQUE constraint.
        stubHappyPath();
        when(users.saveAndFlush(any(User.class)))
                .thenThrow(integrityViolation("users_username_key"));

        // Act + Assert (When/Then)
        assertThatThrownBy(() -> service.register(new RegisterForm("lea", "lea@example.com", "secret12")))
                .isInstanceOf(DuplicateUsernameException.class);
    }

    @Test
    void translates_an_email_constraint_violation_raced_past_the_pre_checks() {
        stubHappyPath();
        when(users.saveAndFlush(any(User.class)))
                .thenThrow(integrityViolation("users_email_key"));

        assertThatThrownBy(() -> service.register(new RegisterForm("lea", "lea@example.com", "secret12")))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void rethrows_an_unrelated_integrity_violation_unmasked() {
        stubHappyPath();
        when(users.saveAndFlush(any(User.class)))
                .thenThrow(integrityViolation("some_other_constraint"));

        assertThatThrownBy(() -> service.register(new RegisterForm("lea", "lea@example.com", "secret12")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void stubHappyPath() {
        Role student = new Role();
        student.setRoleName("STUDENT");

        when(users.existsByUsername("lea")).thenReturn(false);
        when(users.existsByEmail("lea@example.com")).thenReturn(false);
        when(roles.findByRoleName("STUDENT")).thenReturn(Optional.of(student));
        when(encoder.encode("secret12")).thenReturn("HASHED");
        when(users.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    /**
     * Builds the exception Spring raises when an INSERT violates a UNIQUE
     * constraint: a DataIntegrityViolationException wrapping Hibernate's
     * ConstraintViolationException, which carries the constraint name.
     * The SQLException mirrors what PostgreSQL reports: SQLSTATE 23505 is the
     * SQL-standard code for unique_violation.
     */
    private static DataIntegrityViolationException integrityViolation(String constraintName) {
        SQLException uniqueViolation = new SQLException(
                "duplicate key value violates unique constraint \"" + constraintName + "\"",
                "23505");
        return new DataIntegrityViolationException(
                "duplicate key",
                new ConstraintViolationException("duplicate key", uniqueViolation, constraintName));
    }
}
