UPDATE sys_layout_features layout_feature
SET route = '/access-control/client-applications',
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
FROM sys_layout_feature_privileges feature_privilege
JOIN sys_priv_privileges privilege
  ON privilege.id = feature_privilege.privilege_id
WHERE feature_privilege.layout_feature_id = layout_feature.id
  AND feature_privilege.active = true
  AND privilege.privilege_code = '11020100381'
  AND layout_feature.route IS DISTINCT FROM '/access-control/client-applications';
