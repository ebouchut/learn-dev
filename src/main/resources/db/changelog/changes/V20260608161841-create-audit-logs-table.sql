--liquibase formatted sql

-- audit_logs: security audit trail. user_id is nullable (system events) and uses
-- ON DELETE SET NULL so the trail survives user deletion. entity_id is text
-- because it may reference a UUID (users) or a BIGINT (other tables).
--changeset ebouchut:V20260608161841
CREATE TABLE audit_logs (
    log_id         BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    action_type    VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(100),
    entity_id      VARCHAR(64),
    description    TEXT,
    ip_address     INET,
    user_agent     TEXT,
    was_successful BOOLEAN      NOT NULL DEFAULT TRUE,
    error_message  TEXT,
    metadata       JSONB,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    user_id        UUID         REFERENCES users (user_id) ON DELETE SET NULL
);
--rollback DROP TABLE audit_logs;
