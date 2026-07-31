package com.ericbouchut.learndev.auth;

import com.ericbouchut.learndev.support.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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

    /**
     * Renders the registration form and its server-side error state. This
     * guards template regressions the happy-path test cannot see: a template
     * exception surfaces as a redirect to the login page (the error page is
     * behind authentication), not as an obvious 500.
     */
    @Test
    void register_form_renders_and_shows_field_errors() throws Exception {
        // The empty form renders for an anonymous visitor.
        mvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Create an account")));

        // Invalid input re-renders the form with the alert and the error
        // wired to its field (aria-describedby / aria-invalid).
        mvc.perform(post("/auth/register").with(csrf())
                        .param("username", "ab")          // too short (min 3)
                        .param("email", "not-an-email")
                        .param("password", "short"))      // too short (min 8)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("could not be completed")))
                .andExpect(content().string(containsString("aria-invalid=\"true\"")))
                .andExpect(content().string(containsString("id=\"username-error\"")));
    }

    /**
     * The header identifies the signed-in account on every page: sighted
     * users see the username in the account group next to Log out, screen
     * readers hear "Signed in as [name]" (issue #126). Anonymous visitors
     * get no account group at all.
     */
    @Test
    void header_shows_the_signed_in_username() throws Exception {
        mvc.perform(get("/").with(user("carol-header").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("site-header__account")))
                .andExpect(content().string(containsString("Signed in as")))
                .andExpect(content().string(containsString("carol-header")));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("site-header__account"))));
    }
}
