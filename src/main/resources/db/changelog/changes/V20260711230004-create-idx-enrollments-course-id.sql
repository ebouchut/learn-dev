--liquibase formatted sql

-- Index on the enrollments.course_id foreign key (the composite PK leads
-- with user_id, so course_id lookups need their own index).
--changeset ebouchut:V20260711230004
CREATE INDEX idx_enrollments_course_id ON enrollments (course_id);
--rollback DROP INDEX idx_enrollments_course_id;
