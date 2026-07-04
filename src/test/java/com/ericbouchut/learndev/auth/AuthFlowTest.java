package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test for the authentication flow. Boots the full
 * application context against the shared Postgres container and drives the
 * journey through MockMvc: a protected page redirects when anonymous, a new
 * account can register, wrong credentials are rejected, and correct credentials
 * authenticate and land on the dashboard.
 *
 * <p>Named with the {@code Test} suffix (not {@code IT}) so Surefire runs it as
 * part of {@code mvn test}; this project does not use the Failsafe plugin.
 */
@SpringBootTest(properties = {
        // This feature does not use MongoDB; keep the test context Postgres-only.
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
})
@AutoConfigureMockMvc
class AuthFlowTest extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Test
    void register_then_login_then_reach_dashboard() throws Exception {
        // A protected page redirects to the login page when anonymous.
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));

        // Register a new account (CSRF token required for the POST).
        mvc.perform(post("/auth/register").with(csrf())
                        .param("username", "carol")
                        .param("email", "carol@example.com")
                        .param("password", "secret12"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?registered"));

        // A wrong password is rejected.
        mvc.perform(formLogin("/auth/login").user("carol").password("wrong"))
                .andExpect(unauthenticated());

        // Correct credentials authenticate and redirect to the dashboard.
        mvc.perform(formLogin("/auth/login").user("carol").password("secret12"))
                .andExpect(authenticated().withUsername("carol"))
                .andExpect(redirectedUrl("/dashboard"));
    }
}
