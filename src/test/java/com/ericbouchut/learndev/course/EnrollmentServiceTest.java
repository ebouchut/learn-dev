package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.Enrollment;
import com.ericbouchut.learndev.course.entity.EnrollmentStatus;
import com.ericbouchut.learndev.course.entity.PublicationStatus;
import com.ericbouchut.learndev.course.repository.EnrollmentRepository;
import com.ericbouchut.learndev.user.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EnrollmentServiceTest {

    private final EnrollmentRepository enrollments = mock(EnrollmentRepository.class);
    private final EnrollmentService service = new EnrollmentService(enrollments);

    private final User student = studentWithId();
    private final Course course = publishedCourse();

    private static User studentWithId() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUsername("student");
        return user;
    }

    private static Course publishedCourse() {
        Course c = new Course();
        c.setCourseId(42L);
        c.setTitle("Course");
        c.setStatus(PublicationStatus.PUBLISHED);
        return c;
    }

    @Test
    void enrolls_a_new_student_in_a_published_course() {
        // Arrange (Given): no enrollment row yet
        when(enrollments.findByUserAndCourse(student, course)).thenReturn(Optional.empty());

        // Act (When)
        service.enroll(student, course);

        // Assert (Then): a fresh ENROLLED row is saved
        ArgumentCaptor<Enrollment> saved = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollments).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(saved.getValue().getDroppedAt()).isNull();
    }

    @Test
    void enrolling_twice_is_a_no_op() {
        // Arrange (Given): an active enrollment already exists
        Enrollment active = new Enrollment(student, course);
        when(enrollments.findByUserAndCourse(student, course)).thenReturn(Optional.of(active));

        // Act (When)
        service.enroll(student, course);

        // Assert (Then): nothing is written
        verify(enrollments, never()).save(any());
    }

    @Test
    void re_enrolling_reactivates_a_dropped_row_and_clears_the_drop_timestamp() {
        // Arrange (Given): the student dropped the course earlier
        Enrollment dropped = new Enrollment(student, course);
        dropped.setStatus(EnrollmentStatus.DROPPED);
        dropped.setDroppedAt(OffsetDateTime.now().minusDays(3));
        when(enrollments.findByUserAndCourse(student, course)).thenReturn(Optional.of(dropped));

        // Act (When)
        service.enroll(student, course);

        // Assert (Then): the same row flips back to ENROLLED
        verify(enrollments).save(dropped);
        assertThat(dropped.getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(dropped.getDroppedAt()).isNull();
    }

    @Test
    void refuses_to_enroll_in_an_unpublished_course() {
        // Arrange (Given): the course is only a draft
        Course draft = publishedCourse();
        draft.setStatus(PublicationStatus.DRAFT);
        when(enrollments.findByUserAndCourse(student, draft)).thenReturn(Optional.empty());

        // Act + Assert (When/Then): surfaces as 404, drafts do not leak
        assertThatThrownBy(() -> service.enroll(student, draft))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        verify(enrollments, never()).save(any());
    }

    @Test
    void dropping_sets_the_status_and_the_drop_timestamp() {
        // Arrange (Given): an active enrollment
        Enrollment active = new Enrollment(student, course);
        when(enrollments.findByUserAndCourse(student, course)).thenReturn(Optional.of(active));

        // Act (When)
        service.drop(student, course);

        // Assert (Then)
        verify(enrollments).save(active);
        assertThat(active.getStatus()).isEqualTo(EnrollmentStatus.DROPPED);
        assertThat(active.getDroppedAt()).isNotNull();
    }

    @Test
    void dropping_a_completed_course_is_a_no_op() {
        // Arrange (Given): the lifecycle has no transition out of COMPLETED
        Enrollment completed = new Enrollment(student, course);
        completed.setStatus(EnrollmentStatus.COMPLETED);
        when(enrollments.findByUserAndCourse(student, course)).thenReturn(Optional.of(completed));

        // Act (When)
        service.drop(student, course);

        // Assert (Then): nothing is written
        verify(enrollments, never()).save(any());
        assertThat(completed.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
    }
}
