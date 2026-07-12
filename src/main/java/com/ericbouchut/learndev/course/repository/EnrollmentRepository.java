package com.ericbouchut.learndev.course.repository;

import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.Enrollment;
import com.ericbouchut.learndev.course.entity.EnrollmentId;
import com.ericbouchut.learndev.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, EnrollmentId> {

    /** A student's enrollments (their personal course list). */
    List<Enrollment> findByUser(User user);

    /** The roster of a course. */
    List<Enrollment> findByCourse(Course course);
}
