package com.ericbouchut.learndev.common.config;

import com.ericbouchut.learndev.support.AbstractPostgresIT;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Access matrix for the role-gated URL prefixes: anonymous users are sent to
 * the login page, the wrong role is denied (403), and the right role reaches
 * its area (200).
 *
 * <p>Named with the {@code Test} suffix (not {@code IT}) so Surefire runs it
 * as part of {@code mvn test}; this project does not use the Failsafe plugin.
 */
@SpringBootTest(properties = {
        // This feature does not use MongoDB; keep the test context Postgres-only.
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration"
})
@AutoConfigureMockMvc
class SecurityMatrixTest extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    // The catalogue and instructor controllers resolve the domain user, so
    // the gate-pass checks need real users rows, not just mock principals.
    @BeforeEach
    void seedUsers() {
        seedUser("matrix-student");
        seedUser("matrix-instructor");
    }

    private void seedUser(String username) {
        if (userRepository.findByUsername(username).isEmpty()) {
            User user = new User();
            user.setUsername(username);
            user.setEmail(username + "@example.com");
            user.setPassword("hashed");
            userRepository.save(user);
        }
    }

    // Anonymous: every gated prefix redirects to the login page.

    @Test
    void anonymous_is_redirected_to_login_on_gated_prefixes() throws Exception {
        for (String path : new String[] {"/courses", "/instructor/courses", "/admin/users"}) {
            mvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/auth/login"));
        }
    }

    @Test
    void anonymous_can_fetch_the_root_level_icons() throws Exception {
        // Browsers request tab and bookmark icons outside any page context,
        // often before login; a redirect here breaks the tab icon.
        for (String path : new String[] {"/favicon.svg", "/favicon.ico", "/apple-touch-icon.png"}) {
            mvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    // Student: may pass the /courses gate, denied on instructor and admin.

    @Test
    void student_passes_courses_gate() throws Exception {
        mvc.perform(get("/courses").with(user("matrix-student").roles("STUDENT")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void student_is_denied_instructor_and_admin() throws Exception {
        mvc.perform(get("/instructor/courses")).andExpect(status().isForbidden());
        mvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    }

    // Instructor: may pass the /instructor gate, denied on admin.

    @Test
    void instructor_passes_instructor_gate() throws Exception {
        mvc.perform(get("/instructor/courses")
                        .with(user("matrix-instructor").roles("INSTRUCTOR")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void instructor_is_denied_admin() throws Exception {
        mvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    }

    // Admin: may pass the /admin gate; not an instructor, so denied there.

    @Test
    void admin_passes_admin_gate() throws Exception {
        mvc.perform(get("/admin/users")
                        .with(user("matrix-admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_is_denied_instructor() throws Exception {
        mvc.perform(get("/instructor/courses")).andExpect(status().isForbidden());
    }
}
