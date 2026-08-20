--liquibase formatted sql

-- enrollments: the association between a student and a course, one row per
-- (student, course) pair, so the composite PK IS the identity (no surrogate
-- id) and its index also serves user_id lookups (leading column). The status
-- column implements the persisted part of the student course progress
-- lifecycle (see CONTRIBUTING.md); enrolled_at, completed_at, and dropped_at
-- record when the matching transitions happened.
--changeset ebouchut:V20260711230002
CREATE TABLE enrollments (
    user_id      UUID        NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    course_id    BIGINT      NOT NULL REFERENCES courses (course_id) ON DELETE CASCADE,
    status       VARCHAR(20) NOT NULL DEFAULT 'ENROLLED'
        CONSTRAINT chk_enrollments_status CHECK (status IN ('ENROLLED', 'IN_PROGRESS', 'COMPLETED', 'DROPPED')),
    enrolled_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    dropped_at   TIMESTAMPTZ,
    PRIMARY KEY (user_id, course_id)
);
--rollback DROP TABLE enrollments;
