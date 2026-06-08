--liquibase formatted sql

-- Index on the audit_logs.user_id foreign key.
--changeset ebouchut:V20260608161845
CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
--rollback DROP INDEX idx_audit_logs_user_id;
