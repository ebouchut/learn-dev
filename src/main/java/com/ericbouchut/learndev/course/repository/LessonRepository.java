package com.ericbouchut.learndev.course.repository;

import com.ericbouchut.learndev.course.entity.Course;
import com.ericbouchut.learndev.course.entity.Lesson;
import com.ericbouchut.learndev.course.entity.PublicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /** The lessons of a course in reading order (position ascending). */
    List<Lesson> findByCourseOrderByPositionAsc(Course course);

    /** The lessons of a course in one status, in reading order. */
    List<Lesson> findByCourseAndStatusOrderByPositionAsc(Course course, PublicationStatus status);
}
