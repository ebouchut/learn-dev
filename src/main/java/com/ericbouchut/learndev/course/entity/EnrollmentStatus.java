package com.ericbouchut.learndev.course.entity;

/**
 * Persisted states of the student course progress lifecycle
 * (see the diagram in CONTRIBUTING.md).
 * <p>{@code ENROLLED} covers the diagram's Enrolled and NotStarted states:
 * v1 has no prerequisites, so enrolling succeeds synchronously and the
 * pending/failed states are not stored.
 */
public enum EnrollmentStatus {
    ENROLLED,
    IN_PROGRESS,
    COMPLETED,
    DROPPED
}
