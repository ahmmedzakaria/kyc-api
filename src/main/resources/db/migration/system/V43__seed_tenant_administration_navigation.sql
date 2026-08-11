INSERT INTO sys_layout_features (
    feature_group_id, feature_code, t_code, feature_name, route, icon,
    display_order, active, created_by, updated_by, created_at, updated_at
)
SELECT fg.id, 'TENANT_ADMINISTRATION', 'SYS_TENANT_ADMIN', 'Tenant Administration',
       '/access-control/tenants', 'building', 80, true, 0, 0, now(), now()
FROM sys_layout_feature_groups fg
WHERE fg.feature_group_code = 'ACCESS_CONTROL'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_features feature
      WHERE feature.feature_group_id = fg.id AND feature.feature_code = 'TENANT_ADMINISTRATION'
  );

INSERT INTO sys_layout_feature_privileges (
    layout_feature_id, privilege_id, match_mode, active,
    created_by, updated_by, created_at, updated_at
)
SELECT feature.id, privilege.id, 'ANY', true, 0, 0, now(), now()
FROM sys_layout_features feature
JOIN sys_layout_feature_groups feature_group ON feature_group.id = feature.feature_group_id
JOIN sys_priv_privileges privilege ON privilege.privilege_code = '11020100901'
WHERE feature_group.feature_group_code = 'ACCESS_CONTROL'
  AND feature.feature_code = 'TENANT_ADMINISTRATION'
  AND NOT EXISTS (
      SELECT 1 FROM sys_layout_feature_privileges link
      WHERE link.layout_feature_id = feature.id AND link.privilege_id = privilege.id
  );
