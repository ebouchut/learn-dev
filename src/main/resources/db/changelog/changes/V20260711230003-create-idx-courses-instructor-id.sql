--liquibase formatted sql

-- Index on the courses.instructor_id foreign key.
--changeset ebouchut:V20260711230003
CREATE INDEX idx_courses_instructor_id ON courses (instructor_id);
--rollback DROP INDEX idx_courses_instructor_id;
