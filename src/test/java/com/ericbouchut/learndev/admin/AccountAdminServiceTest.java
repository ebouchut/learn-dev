package com.ericbouchut.learndev.admin;

import com.ericbouchut.learndev.audit.AuditService;
import com.ericbouchut.learndev.auth.RegistrationService;
import com.ericbouchut.learndev.auth.dto.RegisterForm;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AccountAdminServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final RegistrationService registration = mock(RegistrationService.class);
    private final AuditService audit = mock(AuditService.class);
    private final AccountAdminService service =
            new AccountAdminService(users, registration, audit);

    private final User admin = userWithId("admin");
    private final User target = userWithId("someone");

    private static User userWithId(String username) {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUsername(username);
        return user;
    }

    @Test
    void creates_an_instructor_account_and_audits_it() {
        // Arrange (Given): registration succeeds with the INSTRUCTOR role
        RegisterForm form = new RegisterForm("teacher", "teacher@example.com", "secret12");
        when(registration.register(form, "INSTRUCTOR")).thenReturn(target);

        // Act (When)
        User created = service.createInstructor(form, admin, "127.0.0.1");

        // Assert (Then): delegated with the right role, audited
        assertThat(created).isSameAs(target);
        verify(registration).register(form, "INSTRUCTOR");
        verify(audit).record(eq("ACCOUNT_CREATED"), eq(admin), anyString(),
                anyBoolean(), anyString());
    }

    @Test
    void archiving_disables_the_account_and_audits_it() {
        // Act (When)
        service.archive(target, admin, "127.0.0.1");

        // Assert (Then)
        assertThat(target.isActive()).isFalse();
        verify(users).save(target);
        verify(audit).record(eq("ACCOUNT_ARCHIVED"), eq(admin), anyString(),
                anyBoolean(), anyString());
    }

    @Test
    void an_admin_cannot_archive_their_own_account() {
        // Act + Assert (When/Then): 409, nothing written
        assertThatThrownBy(() -> service.archive(admin, admin, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        assertThat(admin.isActive()).isTrue();
        verify(users, never()).save(any());
    }

    @Test
    void reactivating_enables_the_account_again() {
        // Arrange (Given): an archived account
        target.setActive(false);

        // Act (When)
        service.reactivate(target, admin, "127.0.0.1");

        // Assert (Then)
        assertThat(target.isActive()).isTrue();
        verify(audit).record(eq("ACCOUNT_REACTIVATED"), eq(admin), anyString(),
                anyBoolean(), anyString());
    }
}
