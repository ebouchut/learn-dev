package com.ericbouchut.learndev.course.repository;

import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.Enrollment;
import com.ericbouchut.learndev.course.entity.EnrollmentId;
import com.ericbouchut.learndev.course.entity.EnrollmentStatus;
import com.ericbouchut.learndev.course.entity.Lesson;
import com.ericbouchut.learndev.course.entity.PublicationStatus;
import com.ericbouchut.learndev.support.AbstractPostgresIT;
import com.ericbouchut.learndev.user.entity.User;
import com.ericbouchut.learndev.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Persistence tests of the course domain (courses, lessons, enrollments)
 * against the real schema: statuses round-trip as strings, the lesson order
 * is unique per course, the enrollment identity is the (user, course) pair,
 * and deleting a course cascades in the DATABASE (ON DELETE CASCADE), not in
 * Hibernate.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CourseDomainRepositoryTest extends AbstractPostgresIT {

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    LessonRepository lessonRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    TestEntityManager entityManager;

    private User newUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPassword("hashed");
        return userRepository.save(user);
    }

    private Course newCourse(User instructor, String title) {
        Course course = new Course();
        course.setTitle(title);
        course.setInstructor(instructor);
        return courseRepository.save(course);
    }

    private Lesson newLesson(Course course, int position, String title) {
        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setPosition(position);
        lesson.setTitle(title);
        return lessonRepository.save(lesson);
    }

    @Test
    void persists_a_course_with_its_lessons_in_reading_order() {
        // Arrange (Given): a draft course with lessons saved out of order
        User instructor = newUser("instructor1");
        Course course = newCourse(instructor, "PostgreSQL indexing");
        newLesson(course, 2, "Composite indexes");
        newLesson(course, 1, "B-tree basics");

        // Act (When): publish the course and reload everything from the DB
        course.setStatus(PublicationStatus.PUBLISHED);
        courseRepository.saveAndFlush(course);
        entityManager.clear();
        Course reloaded = courseRepository.findById(course.getCourseId()).orElseThrow();
        var lessons = lessonRepository.findByCourseOrderByPositionAsc(reloaded);

        // Assert (Then): the status round-trips and the order is by position
        assertThat(reloaded.getStatus()).isEqualTo(PublicationStatus.PUBLISHED);
        assertThat(lessons)
                .extracting(Lesson::getTitle)
                .containsExactly("B-tree basics", "Composite indexes");
        assertThat(lessons)
                .allSatisfy(lesson -> assertThat(lesson.getStatus()).isEqualTo(PublicationStatus.DRAFT));
    }

    @Test
    void rejects_two_lessons_at_the_same_position_in_a_course() {
        // Arrange (Given): a course with a lesson at position 1
        User instructor = newUser("instructor2");
        Course course = newCourse(instructor, "Duplicate positions");
        newLesson(course, 1, "First");

        // Act + Assert (When/Then): a second lesson at position 1 violates
        // uq_lessons_course_position
        assertThatThrownBy(() -> {
            newLesson(course, 1, "Usurper");
            lessonRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enrollment_identity_is_the_user_and_course_pair() {
        // Arrange (Given): a student enrolled in a course
        User instructor = newUser("instructor3");
        User student = newUser("student3");
        Course course = newCourse(instructor, "Enrollment basics");
        enrollmentRepository.saveAndFlush(new Enrollment(student, course));
        entityManager.clear();

        // Act (When): look the enrollment up by the composite id
        var enrollmentId = new EnrollmentId(student.getUserId(), course.getCourseId());
        var maybeEnrollment = enrollmentRepository.findById(enrollmentId);

        // Assert (Then): found, with the defaults from the lifecycle
        assertThat(maybeEnrollment).isPresent();
        assertThat(maybeEnrollment.get().getStatus()).isEqualTo(EnrollmentStatus.ENROLLED);
        assertThat(maybeEnrollment.get().getEnrolledAt()).isNotNull();
        assertThat(maybeEnrollment.get().getCompletedAt()).isNull();
    }

    @Test
    void deleting_a_course_cascades_to_its_lessons_and_enrollments() {
        // Arrange (Given): a course with a lesson and an enrolled student
        User instructor = newUser("instructor4");
        User student = newUser("student4");
        Course course = newCourse(instructor, "Doomed course");
        Lesson lesson = newLesson(course, 1, "Doomed lesson");
        enrollmentRepository.save(new Enrollment(student, course));
        entityManager.flush();
        entityManager.clear();

        // Act (When): delete the course row from a fresh context (no managed
        // lesson/enrollment referencing it); the DATABASE cascades
        courseRepository.deleteById(course.getCourseId());
        courseRepository.flush();
        entityManager.clear();

        // Assert (Then): the lesson and the enrollment are gone with it
        assertThat(lessonRepository.findById(lesson.getLessonId())).isEmpty();
        assertThat(enrollmentRepository
                .findById(new EnrollmentId(student.getUserId(), course.getCourseId())))
                .isEmpty();
    }

    @Test
    void finds_only_the_published_lessons_of_a_course_in_reading_order() {
        // Arrange (Given): a course with published and draft lessons
        User instructor = newUser("instructor6");
        Course course = newCourse(instructor, "Publication filter");
        Lesson second = newLesson(course, 2, "Published second");
        Lesson first = newLesson(course, 1, "Published first");
        newLesson(course, 3, "Still a draft");
        first.setStatus(PublicationStatus.PUBLISHED);
        second.setStatus(PublicationStatus.PUBLISHED);
        lessonRepository.saveAndFlush(first);
        lessonRepository.saveAndFlush(second);
        entityManager.clear();

        // Act (When): fetch the published lessons only
        Course reloaded = courseRepository.findById(course.getCourseId()).orElseThrow();
        var published = lessonRepository
                .findByCourseAndStatusOrderByPositionAsc(reloaded, PublicationStatus.PUBLISHED);

        // Assert (Then): the draft is filtered out, order is by position
        assertThat(published)
                .extracting(Lesson::getTitle)
                .containsExactly("Published first", "Published second");
    }

    @Test
    void finds_the_enrollment_of_a_user_in_a_course() {
        // Arrange (Given): a student enrolled in one of two courses
        User instructor = newUser("instructor7");
        User student = newUser("student7");
        Course joined = newCourse(instructor, "Joined course");
        Course other = newCourse(instructor, "Other course");
        enrollmentRepository.saveAndFlush(new Enrollment(student, joined));
        entityManager.clear();

        // Act (When): look enrollments up by (user, course)
        User reloadedStudent = userRepository.findById(student.getUserId()).orElseThrow();
        Course reloadedJoined = courseRepository.findById(joined.getCourseId()).orElseThrow();
        Course reloadedOther = courseRepository.findById(other.getCourseId()).orElseThrow();

        // Assert (Then): present for the joined course, empty for the other
        assertThat(enrollmentRepository.findByUserAndCourse(reloadedStudent, reloadedJoined))
                .isPresent();
        assertThat(enrollmentRepository.findByUserAndCourse(reloadedStudent, reloadedOther))
                .isEmpty();
    }

    @Test
    void swapping_two_lesson_positions_through_a_temporary_value_satisfies_the_constraint() {
        // Arrange (Given): two lessons at positions 1 and 2; the unique
        // constraint is NOT deferrable, so a direct swap would collide
        User instructor = newUser("instructor8");
        Course course = newCourse(instructor, "Reorder course");
        Lesson first = newLesson(course, 1, "First");
        Lesson second = newLesson(course, 2, "Second");

        // Act (When): the three-step swap used by InstructorCourseService
        // (A to a temporary negative position, B to A's, A to B's)
        first.setPosition(-1);
        lessonRepository.saveAndFlush(first);
        second.setPosition(1);
        lessonRepository.saveAndFlush(second);
        first.setPosition(2);
        lessonRepository.saveAndFlush(first);
        entityManager.clear();

        // Assert (Then): the reading order is swapped
        Course reloaded = courseRepository.findById(course.getCourseId()).orElseThrow();
        assertThat(lessonRepository.findByCourseOrderByPositionAsc(reloaded))
                .extracting(Lesson::getTitle)
                .containsExactly("Second", "First");
    }

    @Test
    void deleting_an_instructor_with_courses_is_rejected() {
        // Arrange (Given): an instructor who teaches a course
        User instructor = newUser("instructor5");
        newCourse(instructor, "Orphan-proof course");
        entityManager.flush();
        entityManager.clear();

        // Act + Assert (When/Then): courses.instructor_id is ON DELETE
        // RESTRICT, so the user row cannot go while the course exists.
        // The fresh context makes Hibernate issue a plain DELETE instead of
        // balking at the managed course that references the user.
        assertThatThrownBy(() -> {
            userRepository.deleteById(instructor.getUserId());
            userRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }
}
