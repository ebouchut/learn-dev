--liquibase formatted sql

-- Index on the user_roles.role_id foreign key (Postgres does not auto-index FKs).
--changeset ebouchut:V20260608161842
CREATE INDEX idx_user_roles_role_id ON user_roles (role_id);
--rollback DROP INDEX idx_user_roles_role_id;
