package com.ericbouchut.learndev.admin;

import com.ericbouchut.learndev.audit.repository.AuditLogRepository;
import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.PublicationStatus;
import com.ericbouchut.learndev.course.repository.CourseRepository;
import com.ericbouchut.learndev.support.AbstractPostgresIT;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end journey of an admin: create an instructor account (which then
 * really unlocks the instructor area through its DB role), archive an
 * account (whose login dies) and reactivate it (login works again), refuse
 * self-archiving, and moderate another instructor's content. Boots the full
 * context against the shared Postgres container.
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
class AdminFlowTest extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    AuditLogRepository auditLogRepository;

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
    void admin_creates_instructors_and_manages_accounts() throws Exception {
        User admin = registeredUser("chief");

        // Create an instructor account through the admin form.
        mvc.perform(post("/admin/users/new-instructor")
                        .with(user("chief").roles("ADMIN")).with(csrf())
                        .param("username", "new-teacher")
                        .param("email", "new-teacher@example.com")
                        .param("password", "secret12"))
                .andExpect(redirectedUrl("/admin/users?created"));

        // The DB role is real: logging in as the new instructor unlocks the
        // instructor area with no mock authorities involved.
        mvc.perform(formLogin("/auth/login").user("new-teacher").password("secret12"))
                .andExpect(authenticated().withUsername("new-teacher").withRoles("INSTRUCTOR"));
        User teacher = userRepository.findByUsername("new-teacher").orElseThrow();
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> "ACCOUNT_CREATED".equals(log.getActionType()));

        // A duplicate username re-renders the form with a field error.
        mvc.perform(post("/admin/users/new-instructor")
                        .with(user("chief").roles("ADMIN")).with(csrf())
                        .param("username", "new-teacher")
                        .param("email", "other@example.com")
                        .param("password", "secret12"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Username already taken")));

        // Archive the instructor: their login dies.
        mvc.perform(post("/admin/users/{id}/archive", teacher.getUserId())
                        .with(user("chief").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/users?archived"));
        mvc.perform(formLogin("/auth/login").user("new-teacher").password("secret12"))
                .andExpect(unauthenticated());

        // Reactivate: the login works again.
        mvc.perform(post("/admin/users/{id}/reactivate", teacher.getUserId())
                        .with(user("chief").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/users?reactivated"));
        mvc.perform(formLogin("/auth/login").user("new-teacher").password("secret12"))
                .andExpect(authenticated().withUsername("new-teacher"));

        // Self-archiving is refused.
        mvc.perform(post("/admin/users/{id}/archive", admin.getUserId())
                        .with(user("chief").roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void admin_moderates_any_instructors_content() throws Exception {
        registeredUser("moderator");
        User owner = registeredUser("course-owner");

        Course course = new Course();
        course.setTitle("Course to moderate");
        course.setStatus(PublicationStatus.PUBLISHED);
        course.setInstructor(owner);
        courseRepository.save(course);

        // The moderation view lists the course; archiving it needs no
        // ownership (the admin is not the instructor).
        mvc.perform(get("/admin/courses").with(user("moderator").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Course to moderate")));
        mvc.perform(post("/admin/courses/{id}/archive", course.getCourseId())
                        .with(user("moderator").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/courses?archived"));
        assertThat(courseRepository.findById(course.getCourseId()).orElseThrow().getStatus())
                .isEqualTo(PublicationStatus.ARCHIVED);

        // Restore brings it back as a draft.
        mvc.perform(post("/admin/courses/{id}/restore", course.getCourseId())
                        .with(user("moderator").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/courses?restored"));
        assertThat(courseRepository.findById(course.getCourseId()).orElseThrow().getStatus())
                .isEqualTo(PublicationStatus.DRAFT);

        // The admin role does not leak into the instructor area.
        mvc.perform(get("/instructor/courses").with(user("moderator").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }
}
