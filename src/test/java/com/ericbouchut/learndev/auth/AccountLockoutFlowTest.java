package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.support.AbstractPostgresIT;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end account lockout: repeated wrong passwords lock the account at
 * the configured threshold, the right password no longer helps, an admin
 * unlock clears the lock and the counter, and a successful login resets the
 * counter and stamps {@code last_login_at}.
 *
 * <p>Named with the {@code Test} suffix (not {@code IT}) so Surefire runs it
 * as part of {@code mvn test}; this project does not use the Failsafe plugin.
 */
@SpringBootTest(properties = {
        // This feature does not use MongoDB; keep the test context Postgres-only.
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration",
        // A low threshold keeps the test readable; production uses 5.
        "learndev.lockout.max-attempts=3"
})
@AutoConfigureMockMvc
class AccountLockoutFlowTest extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    private User registeredUser(String username) throws Exception {
        if (userRepository.findByUsername(username).isEmpty()) {
            mvc.perform(post("/auth/register").with(csrf())
                            .param("username", username)
                            .param("email", username + "@example.com")
                            .param("password", "secret12"))
                    .andExpect(status().is3xxRedirection());
        }
        return userRepository.findByUsername(username).orElseThrow();
    }

    @Test
    void repeated_failures_lock_the_account_and_the_admin_unlocks_it() throws Exception {
        User victim = registeredUser("clumsy");
        registeredUser("boss");

        // Two wrong passwords: counted, not locked yet.
        for (int i = 0; i < 2; i++) {
            mvc.perform(formLogin("/auth/login").user("clumsy").password("wrong"))
                    .andExpect(unauthenticated());
        }
        assertThat(userRepository.findByUsername("clumsy").orElseThrow()
                .getFailedLoginAttempts()).isEqualTo(2);

        // Third strike: locked; even the correct password is now rejected.
        mvc.perform(formLogin("/auth/login").user("clumsy").password("wrong"))
                .andExpect(unauthenticated());
        assertThat(userRepository.findByUsername("clumsy").orElseThrow().isLocked()).isTrue();
        mvc.perform(formLogin("/auth/login").user("clumsy").password("secret12"))
                .andExpect(unauthenticated());

        // The admin unlocks the account: lock and counter are cleared.
        mvc.perform(post("/admin/users/{id}/unlock", victim.getUserId())
                        .with(user("boss").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/users?unlocked"));
        User unlocked = userRepository.findByUsername("clumsy").orElseThrow();
        assertThat(unlocked.isLocked()).isFalse();
        assertThat(unlocked.getFailedLoginAttempts()).isZero();

        // The correct password works again, resets the counter, and stamps
        // the login time.
        mvc.perform(formLogin("/auth/login").user("clumsy").password("secret12"))
                .andExpect(authenticated().withUsername("clumsy"));
        assertThat(userRepository.findByUsername("clumsy").orElseThrow().getLastLoginAt())
                .isNotNull();
    }

    @Test
    void failures_for_unknown_usernames_change_nothing() throws Exception {
        // No account row, no counter: the response is indistinguishable.
        mvc.perform(formLogin("/auth/login").user("ghost").password("whatever"))
                .andExpect(unauthenticated());
        assertThat(userRepository.findByUsername("ghost")).isEmpty();
    }
}
