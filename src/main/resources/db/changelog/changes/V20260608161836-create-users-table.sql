--liquibase formatted sql

-- users: application accounts. UUID PK — non-enumerable and future-API-safe (ADR-0003).
--changeset ebouchut:V20260608161836
CREATE TABLE users (
    user_id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    username              VARCHAR(50)  NOT NULL UNIQUE,
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password              VARCHAR(255) NOT NULL,          -- bcrypt/argon2 hash, never plaintext
    first_name            VARCHAR(100),
    last_name             VARCHAR(100),
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    is_verified           BOOLEAN      NOT NULL DEFAULT FALSE,
    is_locked             BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER      NOT NULL DEFAULT 0,
    last_login_at         TIMESTAMPTZ,
    password_changed_at   TIMESTAMPTZ
);
--rollback DROP TABLE users;
