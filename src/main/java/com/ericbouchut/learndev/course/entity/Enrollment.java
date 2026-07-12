package com.ericbouchut.learndev.course.entity;

import com.ericbouchut.learndev.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Join entity linking a student to a course they joined, with state of its
 * own (status and transition timestamps), which is why the association is an
 * entity rather than a bare {@code @ManyToMany} like user_roles.
 * <p>{@code @MapsId} reuses the embedded id's columns for the two
 * associations, so the entity exposes real {@link User} and {@link Course}
 * references while the primary key stays the (user, course) pair.
 */
@Entity
@Table(name = "enrollments")
@Getter
@Setter
@NoArgsConstructor
public class Enrollment {

    @EmbeddedId
    private EnrollmentId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @MapsId("courseId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id")
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

    @Column(name = "enrolled_at", nullable = false)
    private OffsetDateTime enrolledAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "dropped_at")
    private OffsetDateTime droppedAt;

    public Enrollment(User user, Course course) {
        this.id = new EnrollmentId(user.getUserId(), course.getCourseId());
        this.user = user;
        this.course = course;
    }
}
