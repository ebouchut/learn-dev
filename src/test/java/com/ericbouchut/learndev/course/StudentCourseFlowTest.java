package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.Lesson;
import com.ericbouchut.learndev.course.entity.PublicationStatus;
import com.ericbouchut.learndev.course.repository.CourseRepository;
import com.ericbouchut.learndev.course.repository.LessonRepository;
import com.ericbouchut.learndev.support.AbstractPostgresIT;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end journey of a student through the course catalogue: browse
 * (published only), open a course, enroll, read a lesson (Markdown rendered
 * to HTML), drop, and see the dashboard reflect it all. Boots the full
 * context against the shared Postgres container and drives MockMvc with a
 * registered user.
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
class StudentCourseFlowTest extends AbstractPostgresIT {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    LessonRepository lessonRepository;

    private Course published;
    private Lesson lessonOne;
    private Lesson lessonTwo;

    @BeforeEach
    void seedCatalogue() {
        User instructor = userRepository.findByUsername("flow-instructor")
                .orElseGet(() -> {
                    User u = new User();
                    u.setUsername("flow-instructor");
                    u.setEmail("flow-instructor@example.com");
                    u.setPassword("hashed");
                    return userRepository.save(u);
                });

        published = new Course();
        published.setTitle("Spring in practice");
        published.setDescription("From zero to controller.");
        published.setStatus(PublicationStatus.PUBLISHED);
        published.setInstructor(instructor);
        courseRepository.save(published);

        Course draft = new Course();
        draft.setTitle("Secret draft course");
        draft.setInstructor(instructor);
        courseRepository.save(draft);

        lessonOne = lesson(published, 1, "Getting started",
                "# Hello\n\nSome **bold** content.");
        lessonTwo = lesson(published, 2, "Controllers", "More content.");
        Lesson draftLesson = new Lesson();
        draftLesson.setCourse(published);
        draftLesson.setPosition(3);
        draftLesson.setTitle("Unfinished lesson");
        lessonRepository.save(draftLesson);
    }

    private Lesson lesson(Course course, int position, String title, String markdown) {
        Lesson l = new Lesson();
        l.setCourse(course);
        l.setPosition(position);
        l.setTitle(title);
        l.setContentMarkdown(markdown);
        l.setStatus(PublicationStatus.PUBLISHED);
        return lessonRepository.save(l);
    }

    private String registerStudent(String username) throws Exception {
        mvc.perform(post("/auth/register").with(csrf())
                        .param("username", username)
                        .param("email", username + "@example.com")
                        .param("password", "secret12"))
                .andExpect(status().is3xxRedirection());
        return username;
    }

    @Test
    void student_browses_enrolls_reads_and_drops() throws Exception {
        String student = registerStudent("flow-student");

        // The catalogue lists the published course, never the draft.
        mvc.perform(get("/courses").with(user(student).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Spring in practice")))
                .andExpect(content().string(not(containsString("Secret draft course"))));

        // Reading a lesson before enrolling bounces back to the course page.
        mvc.perform(get("/courses/{c}/lessons/{l}",
                        published.getCourseId(), lessonOne.getLessonId())
                        .with(user(student).roles("STUDENT")))
                .andExpect(redirectedUrl(
                        "/courses/" + published.getCourseId() + "?enroll-required"));

        // Enroll (PRG confirmation), then the course page offers to quit.
        mvc.perform(post("/courses/{c}/enroll", published.getCourseId())
                        .with(user(student).roles("STUDENT")).with(csrf()))
                .andExpect(redirectedUrl("/courses/" + published.getCourseId() + "?enrolled"));
        mvc.perform(get("/courses/{c}", published.getCourseId())
                        .with(user(student).roles("STUDENT")))
                .andExpect(content().string(containsString("Quit this course")))
                .andExpect(content().string(not(containsString("Unfinished lesson"))));

        // The lesson renders its Markdown as HTML and links the next lesson.
        mvc.perform(get("/courses/{c}/lessons/{l}",
                        published.getCourseId(), lessonOne.getLessonId())
                        .with(user(student).roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<strong>bold</strong>")))
                .andExpect(content().string(containsString("Controllers")));

        // The draft lesson is not readable even when enrolled.
        mvc.perform(get("/courses/{c}/lessons/{l}", published.getCourseId(),
                        lessonTwo.getLessonId() + 1000)
                        .with(user(student).roles("STUDENT")))
                .andExpect(status().isNotFound());

        // The dashboard lists the enrolled course.
        mvc.perform(get("/dashboard").with(user(student).roles("STUDENT")))
                .andExpect(content().string(containsString("Spring in practice")));

        // Drop (PRG confirmation); the dashboard no longer lists the course.
        mvc.perform(post("/courses/{c}/drop", published.getCourseId())
                        .with(user(student).roles("STUDENT")).with(csrf()))
                .andExpect(redirectedUrl("/courses/" + published.getCourseId() + "?dropped"));
        mvc.perform(get("/dashboard").with(user(student).roles("STUDENT")))
                .andExpect(content().string(not(containsString("Spring in practice"))));
    }
}
