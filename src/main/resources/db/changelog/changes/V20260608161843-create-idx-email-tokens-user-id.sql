--liquibase formatted sql

-- Index on the email_tokens.user_id foreign key.
--changeset ebouchut:V20260608161843
CREATE INDEX idx_email_tokens_user_id ON email_tokens (user_id);
--rollback DROP INDEX idx_email_tokens_user_id;
