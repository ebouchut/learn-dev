--liquibase formatted sql

-- email_tokens: email-verification tokens. The secret is the random `token` column.
--changeset ebouchut:V20260608161839
CREATE TABLE email_tokens (
    token_id   BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ  NOT NULL,
    used_at    TIMESTAMPTZ,
    user_id    UUID         NOT NULL REFERENCES users (user_id) ON DELETE CASCADE
);
--rollback DROP TABLE email_tokens;
