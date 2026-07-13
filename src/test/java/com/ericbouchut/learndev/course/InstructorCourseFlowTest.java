package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.audit.repository.AuditLogRepository;
import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.EnrollmentStatus;
import com.ericbouchut.learndev.course.entity.Lesson;
import com.ericbouchut.learndev.course.entity.PublicationStatus;
import com.ericbouchut.learndev.course.repository.CourseRepository;
import com.ericbouchut.learndev.course.repository.EnrollmentRepository;
import com.ericbouchut.learndev.course.repository.LessonRepository;
import com.ericbouchut.learndev.support.AbstractPostgresIT;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end journey of an instructor through the authoring area: create a
 * course, add and reorder lessons, publish, watch the student side see it,
 * enforce ownership against another instructor, and manage the roster.
 * Boots the full context against the shared Postgres container.
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
class InstructorCourseFlowTest extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    LessonRepository lessonRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

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

    private UserRequestPostProcessor asInstructor(String username) {
        return user(username).roles("INSTRUCTOR");
    }

    @Test
    void instructor_authors_publishes_and_manages_the_roster() throws Exception {
        registeredUser("author1");
        registeredUser("author2");
        User student = registeredUser("learner1");

        // Create a course: lands on the editor as a draft.
        String editUrl = mvc.perform(post("/instructor/courses/new")
                        .with(asInstructor("author1")).with(csrf())
                        .param("title", "Java from scratch")
                        .param("description", "Objects, tests, and taste."))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getRedirectedUrl();
        assertThat(editUrl).matches("/instructor/courses/\\d+/edit\\?created");
        Long courseId = Long.parseLong(editUrl.replaceAll("\\D+", ""));

        // A blank title is rejected with a re-rendered form.
        mvc.perform(post("/instructor/courses/new")
                        .with(asInstructor("author1")).with(csrf())
                        .param("title", "   "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("form__error")));

        // Add two lessons; they take positions 1 and 2.
        mvc.perform(post("/instructor/courses/{c}/lessons/new", courseId)
                        .with(asInstructor("author1")).with(csrf())
                        .param("title", "Variables")
                        .param("contentMarkdown", "Some **basics**."))
                .andExpect(redirectedUrl(editUrl.replace("?created", "?lesson-created")));
        mvc.perform(post("/instructor/courses/{c}/lessons/new", courseId)
                        .with(asInstructor("author1")).with(csrf())
                        .param("title", "Objects")
                        .param("contentMarkdown", "More."))
                .andExpect(status().is3xxRedirection());

        Course course = courseRepository.findById(courseId).orElseThrow();
        List<Lesson> ordered = lessonRepository.findByCourseOrderByPositionAsc(course);
        assertThat(ordered).extracting(Lesson::getTitle)
                .containsExactly("Variables", "Objects");

        // Move the second lesson up: the reading order swaps.
        mvc.perform(post("/instructor/courses/{c}/lessons/{l}/move-up",
                        courseId, ordered.get(1).getLessonId())
                        .with(asInstructor("author1")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(lessonRepository.findByCourseOrderByPositionAsc(course))
                .extracting(Lesson::getTitle)
                .containsExactly("Objects", "Variables");

        // Publish a lesson and the course; published_at gets stamped.
        mvc.perform(post("/instructor/courses/{c}/lessons/{l}/publish",
                        courseId, ordered.get(1).getLessonId())
                        .with(asInstructor("author1")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(post("/instructor/courses/{c}/publish", courseId)
                        .with(asInstructor("author1")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(courseRepository.findById(courseId).orElseThrow().getPublishedAt())
                .isNotNull();

        // The published course reaches the student catalogue.
        mvc.perform(get("/courses").with(user("learner1").roles("STUDENT")))
                .andExpect(content().string(containsString("Java from scratch")));

        // Publishing an already published course is a 409.
        mvc.perform(post("/instructor/courses/{c}/publish", courseId)
                        .with(asInstructor("author1")).with(csrf()))
                .andExpect(status().isConflict());

        // Another instructor cannot touch the course.
        mvc.perform(get("/instructor/courses/{c}/edit", courseId)
                        .with(asInstructor("author2")))
                .andExpect(status().isForbidden());

        // A student enrolls; the roster lists them.
        mvc.perform(post("/courses/{c}/enroll", courseId)
                        .with(user("learner1").roles("STUDENT")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/instructor/courses/{c}/students", courseId)
                        .with(asInstructor("author1")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("learner1")));

        // The instructor removes the student: DROPPED and audited.
        mvc.perform(post("/instructor/courses/{c}/students/{u}/drop",
                        courseId, student.getUserId())
                        .with(asInstructor("author1")).with(csrf()))
                .andExpect(redirectedUrl(
                        "/instructor/courses/" + courseId + "/students?dropped"));
        assertThat(enrollmentRepository
                .findByUserAndCourse(student, course).orElseThrow().getStatus())
                .isEqualTo(EnrollmentStatus.DROPPED);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> "ENROLLMENT_DROPPED_BY_INSTRUCTOR".equals(log.getActionType()));

        // A draft course never reaches the student catalogue.
        mvc.perform(post("/instructor/courses/new")
                        .with(asInstructor("author1")).with(csrf())
                        .param("title", "Secret draft class"))
                .andExpect(status().is3xxRedirection());
        mvc.perform(get("/courses").with(user("learner1").roles("STUDENT")))
                .andExpect(content().string(not(containsString("Secret draft class"))));
    }

    @Test
    void archived_course_can_be_restored_to_draft() throws Exception {
        registeredUser("author3");

        String editUrl = mvc.perform(post("/instructor/courses/new")
                        .with(asInstructor("author3")).with(csrf())
                        .param("title", "Pause me"))
                .andReturn().getResponse().getRedirectedUrl();
        Long courseId = Long.parseLong(editUrl.replaceAll("\\D+", ""));

        mvc.perform(post("/instructor/courses/{c}/archive", courseId)
                        .with(asInstructor("author3")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(courseRepository.findById(courseId).orElseThrow().getStatus())
                .isEqualTo(PublicationStatus.ARCHIVED);

        mvc.perform(post("/instructor/courses/{c}/restore", courseId)
                        .with(asInstructor("author3")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(courseRepository.findById(courseId).orElseThrow().getStatus())
                .isEqualTo(PublicationStatus.DRAFT);
    }
}
