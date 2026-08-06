-- The Person list navigation entry is visible to roles with Person View.
-- Search remains an API/action privilege and is not required by the menu guard.

UPDATE sys_layout_feature_privileges link
SET active = false,
    updated_by = 0,
    updated_at = now()
FROM sys_layout_features feature
WHERE link.layout_feature_id = feature.id
  AND feature.route = '/person'
  AND link.privilege_id <> (
      SELECT privilege.id
      FROM sys_priv_privileges privilege
      WHERE privilege.privilege_code = '01010200101'
  );

INSERT INTO sys_layout_feature_privileges (
    layout_feature_id, privilege_id, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT feature.id, privilege.id, 'ANY', true,
       0, 0, now(), now()
FROM sys_layout_features feature
JOIN sys_priv_privileges privilege
  ON privilege.privilege_code = '01010200101'
WHERE feature.route = '/person'
ON CONFLICT (layout_feature_id, privilege_id) DO UPDATE
SET match_mode = 'ANY',
    active = true,
    updated_by = 0,
    updated_at = now();
