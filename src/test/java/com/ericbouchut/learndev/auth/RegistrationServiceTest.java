package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.auth.dto.RegisterForm;
import com.ericbouchut.learndev.auth.exception.DuplicateEmailException;
import com.ericbouchut.learndev.role.entity.Role;
import com.ericbouchut.learndev.role.repository.RoleRepository;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

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
        Role student = new Role();
        student.setRoleName("STUDENT");
        when(users.existsByUsername("lea")).thenReturn(false);
        when(users.existsByEmail("lea@example.com")).thenReturn(false);
        when(roles.findByRoleName("STUDENT")).thenReturn(Optional.of(student));
        when(encoder.encode("secret12")).thenReturn("HASHED");
        when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

        // Act (When)
        User created = service.register(new RegisterForm("lea", "lea@example.com", "secret12"));

        // Assert (Then)
        assertThat(created.getPassword()).isEqualTo("HASHED");                 // hashed, not raw
        assertThat(created.getRoles()).extracting(Role::getRoleName).containsExactly("STUDENT");
        verify(users).save(any(User.class));
    }

    @Test
    void rejects_a_duplicate_email() {
        when(users.existsByUsername("lea")).thenReturn(false);
        when(users.existsByEmail("lea@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterForm("lea", "lea@example.com", "secret12")))
                .isInstanceOf(DuplicateEmailException.class);
        verify(users, never()).save(any());
    }
}
