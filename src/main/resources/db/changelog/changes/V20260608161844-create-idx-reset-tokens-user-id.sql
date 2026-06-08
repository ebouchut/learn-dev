--liquibase formatted sql

-- Index on the reset_tokens.user_id foreign key.
--changeset ebouchut:V20260608161844
CREATE INDEX idx_reset_tokens_user_id ON reset_tokens (user_id);
--rollback DROP INDEX idx_reset_tokens_user_id;
