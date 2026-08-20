package com.ericbouchut.learndev.course.entity;

/**
 * Editorial lifecycle shared by courses and lessons (see the Course and
 * Lesson lifecycle diagrams in CONTRIBUTING.md).
 * <p>Removal is modeled as {@code ARCHIVED}: v1 has no destructive delete,
 * and the transient "Updated" state of the diagrams is just an edit of a
 * {@code PUBLISHED} row, not a stored status.
 */
public enum PublicationStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
