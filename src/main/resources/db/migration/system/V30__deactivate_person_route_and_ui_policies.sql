-- Deactivates (does not delete) the GLOBAL (client_application_id NULL) route/UI policy
-- rows V26 seeded for the Person module, across all 4 tables it touched. V26 itself stays
-- in place as immutable history — deleting an already-applied Flyway migration file breaks
-- validation on any database that already ran it (fails with "Detected applied migration
-- not resolved locally"), so the row-level effect is undone here instead, the same way
-- every other reversal in this migration set is done.
-- Matches V26's exact route_url/action_code sets so only the rows it created are touched.

UPDATE sys_layout_route_policies
SET active = false, updated_by = 0, updated_at = now()
WHERE client_application_id IS NULL
  AND active = true
  AND route_url IN (
      '/person',
      '/person/create',
      '/person/:id/edit',
      '/person/:id/preview'
  );

UPDATE sys_layout_route_policy_privileges rpp
SET active = false, updated_by = 0, updated_at = now()
FROM sys_layout_route_policies policy
WHERE rpp.route_policy_id = policy.id
  AND rpp.active = true
  AND policy.client_application_id IS NULL
  AND policy.route_url IN (
      '/person',
      '/person/create',
      '/person/:id/edit',
      '/person/:id/preview'
  );

UPDATE sys_layout_ui_policies
SET active = false, updated_by = 0, updated_at = now()
WHERE client_application_id IS NULL
  AND active = true
  AND action_code IN (
      'person.list.add-button',
      'person.list.preview-button',
      'person.list.edit-button',
      'person.list.delete-button',
      'person.preview.edit-button'
  );

UPDATE sys_layout_ui_policy_privileges upp
SET active = false, updated_by = 0, updated_at = now()
FROM sys_layout_ui_policies policy
WHERE upp.ui_policy_id = policy.id
  AND upp.active = true
  AND policy.client_application_id IS NULL
  AND policy.action_code IN (
      'person.list.add-button',
      'person.list.preview-button',
      'person.list.edit-button',
      'person.list.delete-button',
      'person.preview.edit-button'
  );
