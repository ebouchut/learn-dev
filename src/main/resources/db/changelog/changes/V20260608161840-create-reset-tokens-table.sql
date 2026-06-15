--liquibase formatted sql

-- reset_tokens: password-reset tokens. Like email_tokens, plus the requester's IP.
--changeset ebouchut:V20260608161840
CREATE TABLE reset_tokens (
    token_id   BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ,
    ip_address INET,
    user_id    UUID         NOT NULL REFERENCES users (user_id) ON DELETE CASCADE
);
--rollback DROP TABLE reset_tokens;
