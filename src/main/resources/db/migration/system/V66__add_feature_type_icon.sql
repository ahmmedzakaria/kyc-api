ALTER TABLE sys_priv_feature_types
    ADD COLUMN icon varchar(80);

UPDATE sys_priv_feature_types
SET icon = CASE feature_type_code
    WHEN '01' THEN 'fa fa-sliders'
    WHEN '02' THEN 'fa fa-briefcase'
    WHEN '03' THEN 'fa fa-chart-line'
END,
    updated_by = 0,
    updated_at = CURRENT_TIMESTAMP
WHERE feature_type_code IN ('01', '02', '03')
  AND (icon IS NULL OR btrim(icon) = '');
