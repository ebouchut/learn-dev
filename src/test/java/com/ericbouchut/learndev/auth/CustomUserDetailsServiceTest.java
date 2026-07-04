package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.role.entity.Role;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomUserDetailsServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final CustomUserDetailsService service = new CustomUserDetailsService(users);

    @Test
    void maps_roles_to_ROLE_authorities() {
        // Arrange (Given): a user with the STUDENT role
        Role student = new Role();
        student.setRoleName("STUDENT");
        User user = new User();
        user.setUsername("lea");
        user.setPassword("hash");
        user.setRoles(Set.of(student));

        when(users.findByUsername("lea")).thenReturn(Optional.of(user));

        // Act (When)
        UserDetails details = service.loadUserByUsername("lea");

        // Assert (Then)
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_STUDENT");
    }

    @Test
    void throws_when_user_is_unknown() {
        when(users.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
