package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.audit.AuditService;
import com.ericbouchut.learndev.course.dto.CourseForm;
import com.ericbouchut.learndev.course.dto.LessonForm;
import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.Lesson;
import com.ericbouchut.learndev.course.entity.PublicationStatus;
import com.ericbouchut.learndev.course.repository.CourseRepository;
import com.ericbouchut.learndev.course.repository.LessonRepository;
import com.ericbouchut.learndev.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Write side of the course domain for instructors: authoring and the
 * publication lifecycle of {@code CONTRIBUTING.md} (publish from DRAFT,
 * archive from DRAFT or PUBLISHED, restore an ARCHIVED item to DRAFT).
 * An instructor manages only their own courses: another instructor's course
 * answers 403 (the course may be public, its existence is not a secret,
 * unlike student-invisible drafts which answer 404 on the student side).
 * Invalid lifecycle transitions answer 409.
 * <p>{@code published_at} is set on the first publish only and kept on a
 * re-publish after restore, so it records when the course first went live.
 */
@Service
public class InstructorCourseService {

    private final CourseRepository courses;
    private final LessonRepository lessons;
    private final AuditService audit;

    public InstructorCourseService(
            CourseRepository courses,
            LessonRepository lessons,
            AuditService audit
    ) {
        this.courses = courses;
        this.lessons = lessons;
        this.audit = audit;
    }

    /** The instructor's own courses, whatever their status. */
    public List<Course> myCourses(User instructor) {
        return courses.findByInstructor(instructor);
    }

    /**
     * The course as manageable by the given instructor: 404 when it does
     * not exist, 403 when it belongs to another instructor.
     */
    public Course ownedCourse(Long courseId, User instructor) {
        Course course = courses.findById(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!course.getInstructor().getUserId().equals(instructor.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return course;
    }

    /** Create a new DRAFT course owned by the instructor. */
    @Transactional
    public Course create(User instructor, CourseForm form) {
        Course course = new Course();
        course.setTitle(form.title());
        course.setDescription(blankToNull(form.description()));
        course.setInstructor(instructor);
        return courses.save(course);
    }

    /** Update the title and description of the instructor's course. */
    @Transactional
    public void update(Course course, CourseForm form) {
        course.setTitle(form.title());
        course.setDescription(blankToNull(form.description()));
        courses.save(course);
    }

    /** Publish a DRAFT course; the first publish stamps {@code published_at}. */
    @Transactional
    public void publish(Course course, User actor, String ipAddress) {
        requireStatus(course.getStatus(), PublicationStatus.DRAFT);
        course.setStatus(PublicationStatus.PUBLISHED);
        if (course.getPublishedAt() == null) {
            course.setPublishedAt(OffsetDateTime.now());
        }
        courses.save(course);
        audit.record("COURSE_PUBLISHED", actor, ipAddress, true,
                "Course " + course.getCourseId() + " published");
    }

    /** Archive a DRAFT or PUBLISHED course (v1 has no destructive delete). */
    @Transactional
    public void archive(Course course, User actor, String ipAddress) {
        requireStatus(course.getStatus(),
                PublicationStatus.DRAFT, PublicationStatus.PUBLISHED);
        course.setStatus(PublicationStatus.ARCHIVED);
        courses.save(course);
        audit.record("COURSE_ARCHIVED", actor, ipAddress, true,
                "Course " + course.getCourseId() + " archived");
    }

    /** Restore an ARCHIVED course to DRAFT ("re-open" in the lifecycle). */
    @Transactional
    public void restore(Course course, User actor, String ipAddress) {
        requireStatus(course.getStatus(), PublicationStatus.ARCHIVED);
        course.setStatus(PublicationStatus.DRAFT);
        courses.save(course);
        audit.record("COURSE_RESTORED", actor, ipAddress, true,
                "Course " + course.getCourseId() + " restored to draft");
    }

    /** The lessons of the course in reading order, all statuses included. */
    public List<Lesson> lessonsOf(Course course) {
        return lessons.findByCourseOrderByPositionAsc(course);
    }

    /**
     * One lesson of the given course, whatever its status: 404 when the
     * lesson does not exist or belongs to another course.
     */
    public Lesson ownedLesson(Course course, Long lessonId) {
        return lessons.findById(lessonId)
                .filter(lesson -> lesson.getCourse().getCourseId().equals(course.getCourseId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    /** Create a DRAFT lesson at the end of the course (position max + 1). */
    @Transactional
    public Lesson createLesson(Course course, LessonForm form) {
        Lesson lesson = new Lesson();
        lesson.setCourse(course);
        lesson.setTitle(form.title());
        lesson.setContentMarkdown(form.contentMarkdown() == null ? "" : form.contentMarkdown());
        lesson.setPosition(nextPosition(course));
        return lessons.save(lesson);
    }

    /** Update the title and Markdown content of a lesson. */
    @Transactional
    public void updateLesson(Lesson lesson, LessonForm form) {
        lesson.setTitle(form.title());
        lesson.setContentMarkdown(form.contentMarkdown() == null ? "" : form.contentMarkdown());
        lessons.save(lesson);
    }

    /** Publish a DRAFT lesson. */
    @Transactional
    public void publishLesson(Lesson lesson, User actor, String ipAddress) {
        requireStatus(lesson.getStatus(), PublicationStatus.DRAFT);
        lesson.setStatus(PublicationStatus.PUBLISHED);
        lessons.save(lesson);
        audit.record("LESSON_PUBLISHED", actor, ipAddress, true,
                "Lesson " + lesson.getLessonId() + " published");
    }

    /** Archive a DRAFT or PUBLISHED lesson. */
    @Transactional
    public void archiveLesson(Lesson lesson, User actor, String ipAddress) {
        requireStatus(lesson.getStatus(),
                PublicationStatus.DRAFT, PublicationStatus.PUBLISHED);
        lesson.setStatus(PublicationStatus.ARCHIVED);
        lessons.save(lesson);
        audit.record("LESSON_ARCHIVED", actor, ipAddress, true,
                "Lesson " + lesson.getLessonId() + " archived");
    }

    /** Restore an ARCHIVED lesson to DRAFT. */
    @Transactional
    public void restoreLesson(Lesson lesson, User actor, String ipAddress) {
        requireStatus(lesson.getStatus(), PublicationStatus.ARCHIVED);
        lesson.setStatus(PublicationStatus.DRAFT);
        lessons.save(lesson);
        audit.record("LESSON_RESTORED", actor, ipAddress, true,
                "Lesson " + lesson.getLessonId() + " restored to draft");
    }

    /** Swap the lesson with the one before it in reading order (no-op at the top). */
    @Transactional
    public void moveLessonUp(Course course, Lesson lesson) {
        swapWithNeighbor(course, lesson, -1);
    }

    /** Swap the lesson with the one after it in reading order (no-op at the bottom). */
    @Transactional
    public void moveLessonDown(Course course, Lesson lesson) {
        swapWithNeighbor(course, lesson, +1);
    }

    /**
     * Swap positions with the previous/next lesson. The DB constraint
     * uq_lessons_course_position is NOT deferrable, so the swap flushes
     * through a temporary negative position: A to -A, B to oldA, A to oldB
     * (instructor positions are always positive, no collision possible).
     */
    private void swapWithNeighbor(Course course, Lesson lesson, int direction) {
        List<Lesson> ordered = lessonsOf(course);
        int index = ordered.indexOf(ordered.stream()
                .filter(l -> l.getLessonId().equals(lesson.getLessonId()))
                .findFirst().orElseThrow());
        int neighborIndex = index + direction;
        if (neighborIndex < 0 || neighborIndex >= ordered.size()) {
            return;
        }
        Lesson current = ordered.get(index);
        Lesson neighbor = ordered.get(neighborIndex);
        int currentPosition = current.getPosition();
        int neighborPosition = neighbor.getPosition();
        current.setPosition(-currentPosition);
        lessons.saveAndFlush(current);
        neighbor.setPosition(currentPosition);
        lessons.saveAndFlush(neighbor);
        current.setPosition(neighborPosition);
        lessons.saveAndFlush(current);
    }

    private int nextPosition(Course course) {
        return lessonsOf(course).stream()
                .mapToInt(Lesson::getPosition)
                .max()
                .orElse(0) + 1;
    }

    private static void requireStatus(PublicationStatus actual, PublicationStatus... allowed) {
        for (PublicationStatus status : allowed) {
            if (actual == status) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Transition not allowed from " + actual);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
