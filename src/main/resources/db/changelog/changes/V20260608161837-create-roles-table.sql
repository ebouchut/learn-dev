--liquibase formatted sql

-- roles: application roles (e.g. STUDENT, INSTRUCTOR, ADMIN). BIGINT identity PK.
--changeset ebouchut:V20260608161837
CREATE TABLE roles (
    role_id     BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_name   VARCHAR(50)  NOT NULL UNIQUE,
    description VARCHAR(255),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE
);
--rollback DROP TABLE roles;
