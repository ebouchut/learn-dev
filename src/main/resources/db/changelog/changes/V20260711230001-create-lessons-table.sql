--liquibase formatted sql

-- lessons: ordered content within a course, authored as Markdown. Deleting a
-- course cascades to its lessons. UNIQUE (course_id, position) keeps the
-- ordering unambiguous and its index doubles as the foreign-key index
-- (course_id is the leading column), so no separate idx_lessons_course_id.
--changeset ebouchut:V20260711230001
CREATE TABLE lessons (
    lesson_id        BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title            VARCHAR(255) NOT NULL,
    content_markdown TEXT         NOT NULL DEFAULT '',
    position         INTEGER      NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'DRAFT'
        CONSTRAINT chk_lessons_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    course_id        BIGINT       NOT NULL REFERENCES courses (course_id) ON DELETE CASCADE,
    CONSTRAINT uq_lessons_course_position UNIQUE (course_id, position)
);
--rollback DROP TABLE lessons;
