--liquibase formatted sql

-- user_roles: junction table for the N..N relationship between users and roles.
--changeset ebouchut:V20260608161838
CREATE TABLE user_roles (
    user_id     UUID        NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    role_id     BIGINT      NOT NULL REFERENCES roles (role_id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    assigned_by UUID        REFERENCES users (user_id) ON DELETE SET NULL,
    PRIMARY KEY (user_id, role_id)
);
--rollback DROP TABLE user_roles;
