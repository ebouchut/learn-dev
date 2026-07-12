package com.ericbouchut.learndev.course.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * Composite primary key of {@link Enrollment}: the (user, course) pair IS the
 * identity, so a student cannot enroll twice in the same course by
 * construction. Serializable and value-equal as the JPA spec requires for
 * embedded ids.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EnrollmentId implements Serializable {

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "course_id")
    private Long courseId;
}
