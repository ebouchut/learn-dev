package com.ericbouchut.learndev.course.repository;

import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.PublicationStatus;
import com.ericbouchut.learndev.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findByInstructor(User instructor);

    /** The public catalogue lists {@code PUBLISHED} courses only. */
    List<Course> findByStatus(PublicationStatus status);
}
