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
 * the login page, the wrong role is denied (403), and the right role passes
 * the gate. The instructor and admin controllers do not exist yet, so "passes
 * the gate" is asserted as 404 (the request reached MVC dispatch); tighten
 * those assertions to 200 as each feature phase lands.
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

    // The catalogue controller resolves the domain user, so the gate-pass
    // check needs a real users row, not just a mock principal.
    @BeforeEach
    void seedStudent() {
        if (userRepository.findByUsername("matrix-student").isEmpty()) {
            User student = new User();
            student.setUsername("matrix-student");
            student.setEmail("matrix-student@example.com");
            student.setPassword("hashed");
            userRepository.save(student);
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
    @WithMockUser(roles = "INSTRUCTOR")
    void instructor_passes_instructor_gate() throws Exception {
        mvc.perform(get("/instructor/courses")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void instructor_is_denied_admin() throws Exception {
        mvc.perform(get("/admin/users")).andExpect(status().isForbidden());
    }

    // Admin: may pass the /admin gate; not an instructor, so denied there.

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_passes_admin_gate() throws Exception {
        mvc.perform(get("/admin/users")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_is_denied_instructor() throws Exception {
        mvc.perform(get("/instructor/courses")).andExpect(status().isForbidden());
    }
}
