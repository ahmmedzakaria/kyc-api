ALTER TABLE sys_layout_ui_policies
    ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

ALTER TABLE sys_layout_features
    ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;

-- Keep the oldest audited link for each feature/privilege pair before enforcing
-- deterministic full-replacement semantics at the database boundary.
DELETE FROM sys_layout_feature_privileges duplicate
USING sys_layout_feature_privileges canonical
WHERE duplicate.layout_feature_id = canonical.layout_feature_id
  AND duplicate.privilege_id = canonical.privilege_id
  AND duplicate.id > canonical.id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_sys_layout_feature_privileges_feature_privilege
    ON sys_layout_feature_privileges (layout_feature_id, privilege_id);
