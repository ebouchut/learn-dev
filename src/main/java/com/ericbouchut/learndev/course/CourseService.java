package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.Lesson;
import com.ericbouchut.learndev.course.entity.PublicationStatus;
import com.ericbouchut.learndev.course.repository.CourseRepository;
import com.ericbouchut.learndev.course.repository.EnrollmentRepository;
import com.ericbouchut.learndev.course.repository.LessonRepository;
import com.ericbouchut.learndev.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Read side of the course catalogue, applying the student visibility rule:
 * a student sees {@code PUBLISHED} courses; a student who is enrolled keeps
 * read access to the course and its published lessons if the course is later
 * {@code ARCHIVED} (content continuity); {@code DRAFT} content is never
 * visible to students. Invisible content surfaces as 404, not 403, so the
 * URL space does not leak which drafts exist.
 */
@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courses;
    private final LessonRepository lessons;
    private final EnrollmentRepository enrollments;

    public CourseService(
            CourseRepository courses,
            LessonRepository lessons,
            EnrollmentRepository enrollments
    ) {
        this.courses = courses;
        this.lessons = lessons;
        this.enrollments = enrollments;
    }

    /** The public catalogue: published courses only. */
    public List<Course> catalogue() {
        return courses.findByStatus(PublicationStatus.PUBLISHED);
    }

    /**
     * The course as visible to the given student, or 404 when the course
     * does not exist or the visibility rule (see class Javadoc) hides it.
     */
    public Course visibleCourse(Long courseId, User student) {
        Course course = courses.findById(courseId)
                .orElseThrow(CourseService::notFound);
        if (course.getStatus() == PublicationStatus.PUBLISHED) {
            return course;
        }
        boolean enrolledInArchived =
                course.getStatus() == PublicationStatus.ARCHIVED
                        && enrollments.findByUserAndCourse(student, course).isPresent();
        if (enrolledInArchived) {
            return course;
        }
        throw notFound();
    }

    /** The lessons of a course a student may read, in reading order. */
    public List<Lesson> publishedLessons(Course course) {
        return lessons.findByCourseAndStatusOrderByPositionAsc(
                course, PublicationStatus.PUBLISHED);
    }

    /**
     * One published lesson of the given course, or 404 when the lesson does
     * not exist, is not published, or belongs to another course.
     */
    public Lesson publishedLesson(Course course, Long lessonId) {
        return lessons.findById(lessonId)
                .filter(lesson -> lesson.getCourse().getCourseId().equals(course.getCourseId()))
                .filter(lesson -> lesson.getStatus() == PublicationStatus.PUBLISHED)
                .orElseThrow(CourseService::notFound);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
}
