package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.audit.AuditService;
import com.ericbouchut.learndev.course.dto.CourseForm;
import com.ericbouchut.learndev.course.dto.LessonForm;
import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.Lesson;
import com.ericbouchut.learndev.course.entity.PublicationStatus;
import com.ericbouchut.learndev.course.repository.CourseRepository;
import com.ericbouchut.learndev.course.repository.EnrollmentRepository;
import com.ericbouchut.learndev.course.repository.LessonRepository;
import com.ericbouchut.learndev.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InstructorCourseServiceTest {

    private final CourseRepository courses = mock(CourseRepository.class);
    private final LessonRepository lessons = mock(LessonRepository.class);
    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final EnrollmentService enrollmentService = mock(EnrollmentService.class);
    private final AuditService audit = mock(AuditService.class);
    private final InstructorCourseService service = new InstructorCourseService(
            courses, lessons, enrollments, enrollmentService, audit);

    private final User owner = userWithId();
    private final User stranger = userWithId();

    private static User userWithId() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        return user;
    }

    private Course draftCourseOf(User instructor) {
        Course course = new Course();
        course.setCourseId(7L);
        course.setTitle("Draft course");
        course.setInstructor(instructor);
        return course;
    }

    @Test
    void refuses_a_course_owned_by_another_instructor() {
        // Arrange (Given): a course owned by someone else
        Course course = draftCourseOf(owner);
        when(courses.findById(7L)).thenReturn(Optional.of(course));

        // Act + Assert (When/Then): 403, ownership is enforced
        assertThatThrownBy(() -> service.ownedCourse(7L, stranger))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test
    void first_publish_stamps_published_at_and_a_republish_keeps_it() {
        // Arrange (Given): a draft that was never published
        Course course = draftCourseOf(owner);

        // Act (When): publish it
        service.publish(course, owner, "127.0.0.1");

        // Assert (Then): published, stamped, audited
        assertThat(course.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        OffsetDateTime firstPublish = course.getPublishedAt();
        assertThat(firstPublish).isNotNull();
        verify(audit).record(eq("COURSE_PUBLISHED"), eq(owner), anyString(),
                anyBoolean(), anyString());

        // Act (When): archive, restore, publish again
        service.archive(course, owner, "127.0.0.1");
        service.restore(course, owner, "127.0.0.1");
        service.publish(course, owner, "127.0.0.1");

        // Assert (Then): the original first-publish timestamp is kept
        assertThat(course.getPublishedAt()).isEqualTo(firstPublish);
    }

    @Test
    void refuses_to_publish_a_published_course() {
        // Arrange (Given): an already published course
        Course course = draftCourseOf(owner);
        course.setStatus(PublicationStatus.PUBLISHED);

        // Act + Assert (When/Then): 409, the lifecycle only publishes drafts
        assertThatThrownBy(() -> service.publish(course, owner, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verify(audit, never()).record(any(), any(), any(), anyBoolean(), any());
    }

    @Test
    void refuses_to_archive_an_archived_course() {
        // Arrange (Given): an archived course
        Course course = draftCourseOf(owner);
        course.setStatus(PublicationStatus.ARCHIVED);

        // Act + Assert (When/Then): 409
        assertThatThrownBy(() -> service.archive(course, owner, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void a_new_lesson_goes_to_the_end_of_the_course() {
        // Arrange (Given): a course whose lessons end at position 4
        Course course = draftCourseOf(owner);
        when(lessons.findByCourseOrderByPositionAsc(course))
                .thenReturn(List.of(lessonAt(1L, 2), lessonAt(2L, 4)));
        when(lessons.save(any(Lesson.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act (When)
        Lesson created = service.createLesson(course, new LessonForm("Outro", null));

        // Assert (Then): appended after the last position, DRAFT, empty content
        assertThat(created.getPosition()).isEqualTo(5);
        assertThat(created.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(created.getContentMarkdown()).isEmpty();
    }

    @Test
    void moving_a_lesson_up_swaps_the_two_positions() {
        // Arrange (Given): lessons at positions 1 and 2
        Course course = draftCourseOf(owner);
        Lesson first = lessonAt(1L, 1);
        Lesson second = lessonAt(2L, 2);
        when(lessons.findByCourseOrderByPositionAsc(course))
                .thenReturn(List.of(first, second));

        // Act (When): move the second lesson up
        service.moveLessonUp(course, second);

        // Assert (Then): positions are exchanged
        assertThat(second.getPosition()).isEqualTo(1);
        assertThat(first.getPosition()).isEqualTo(2);
    }

    @Test
    void moving_the_first_lesson_up_is_a_no_op() {
        // Arrange (Given): the lesson is already first
        Course course = draftCourseOf(owner);
        Lesson first = lessonAt(1L, 1);
        when(lessons.findByCourseOrderByPositionAsc(course))
                .thenReturn(List.of(first, lessonAt(2L, 2)));

        // Act (When)
        service.moveLessonUp(course, first);

        // Assert (Then): nothing changes, nothing is written
        assertThat(first.getPosition()).isEqualTo(1);
        verify(lessons, never()).saveAndFlush(any());
    }

    private static Lesson lessonAt(Long id, int position) {
        Lesson lesson = new Lesson();
        lesson.setLessonId(id);
        lesson.setTitle("Lesson " + id);
        lesson.setPosition(position);
        return lesson;
    }

    @Test
    void creates_a_draft_course_with_a_null_blank_description() {
        // Arrange (Given): a form with a blank description
        when(courses.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act (When)
        Course created = service.create(owner, new CourseForm("New course", "  "));

        // Assert (Then): DRAFT, owned, description normalized to null
        assertThat(created.getStatus()).isEqualTo(PublicationStatus.DRAFT);
        assertThat(created.getInstructor()).isSameAs(owner);
        assertThat(created.getDescription()).isNull();
    }
}
