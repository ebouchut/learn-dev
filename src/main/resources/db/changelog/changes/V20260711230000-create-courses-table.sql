--liquibase formatted sql

-- courses: units of learning content owned by an instructor. BIGINT identity PK
-- (ADR-0003: UUID is for users only). The status column implements the course
-- lifecycle (see CONTRIBUTING.md): DRAFT, PUBLISHED, ARCHIVED; removal is
-- modeled as ARCHIVED, so deleting a row is not part of the v1 lifecycle.
--changeset ebouchut:V20260711230000
CREATE TABLE courses (
    course_id     BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    description   TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT chk_courses_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    published_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    instructor_id UUID         NOT NULL REFERENCES users (user_id) ON DELETE RESTRICT
);
--rollback DROP TABLE courses;
