package com.ericbouchut.learndev.course;

import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.Enrollment;
import com.ericbouchut.learndev.course.entity.EnrollmentStatus;
import com.ericbouchut.learndev.course.entity.PublicationStatus;
import com.ericbouchut.learndev.course.repository.EnrollmentRepository;
import com.ericbouchut.learndev.user.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Student-side enrollment lifecycle (see the diagram in CONTRIBUTING.md).
 * The (user, course) pair is the primary key, so a student has at most one
 * enrollment row per course: enrolling again re-activates a {@code DROPPED}
 * row instead of inserting a second one, and both operations are idempotent
 * (re-posting a form must not fail).
 */
@Service
public class EnrollmentService {

    private final EnrollmentRepository enrollments;

    public EnrollmentService(EnrollmentRepository enrollments) {
        this.enrollments = enrollments;
    }

    /**
     * Enroll the student in a published course. A {@code DROPPED} enrollment
     * is re-activated (back to {@code ENROLLED}, drop timestamp cleared,
     * enrollment timestamp refreshed); an active one is left untouched.
     *
     * @throws ResponseStatusException 404 when the course is not published
     *     (only published courses accept new enrollments)
     */
    @Transactional
    public void enroll(User student, Course course) {
        Optional<Enrollment> existing = enrollments.findByUserAndCourse(student, course);
        if (existing.isPresent() && existing.get().getStatus() != EnrollmentStatus.DROPPED) {
            return;
        }
        if (course.getStatus() != PublicationStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (existing.isPresent()) {
            Enrollment enrollment = existing.get();
            enrollment.setStatus(EnrollmentStatus.ENROLLED);
            enrollment.setDroppedAt(null);
            enrollment.setEnrolledAt(OffsetDateTime.now());
            enrollments.save(enrollment);
        } else {
            enrollments.save(new Enrollment(student, course));
        }
    }

    /**
     * Drop the student from a course: {@code ENROLLED} or
     * {@code IN_PROGRESS} becomes {@code DROPPED} with a drop timestamp.
     * No-op when the student is not enrolled, already dropped, or completed
     * the course (the lifecycle has no transition out of COMPLETED).
     */
    @Transactional
    public void drop(User student, Course course) {
        enrollments.findByUserAndCourse(student, course)
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ENROLLED
                        || enrollment.getStatus() == EnrollmentStatus.IN_PROGRESS)
                .ifPresent(enrollment -> {
                    enrollment.setStatus(EnrollmentStatus.DROPPED);
                    enrollment.setDroppedAt(OffsetDateTime.now());
                    enrollments.save(enrollment);
                });
    }

    /** The enrollment of a student in a course, if any (dropped included). */
    public Optional<Enrollment> enrollmentFor(User student, Course course) {
        return enrollments.findByUserAndCourse(student, course);
    }

    /** All enrollments of a student, dropped included (the dashboard filters). */
    public List<Enrollment> myEnrollments(User student) {
        return enrollments.findByUser(student);
    }
}
