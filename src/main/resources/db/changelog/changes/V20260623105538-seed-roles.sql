--liquibase formatted sql

-- Seed the fixed set of application roles.
-- The SUPERADMIN role is intentionally deferred (YAGNI); it is planned for a
-- later release and tracked in issue #65. When added, use a new migration
-- (do not edit this one: migrations are append-only).
--changeset ebouchut:V20260623105538
INSERT INTO roles (role_name, description) VALUES
    ('STUDENT',    'Learner who follows courses and does exercises'),
    ('INSTRUCTOR', 'Author of courses, lessons and exercises'),
    ('ADMIN',      'Platform administrator');
--rollback DELETE FROM roles WHERE role_name IN ('STUDENT', 'INSTRUCTOR', 'ADMIN');
