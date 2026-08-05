UPDATE sys_layout_navigation_modules
SET description = 'Manage KYC, KYB, AML screening, due diligence, monitoring, case investigation, and regulatory reporting workflows.',
    updated_by = 0,
    updated_at = now()
WHERE navigation_module_code = 'KYC'
  AND description IS DISTINCT FROM 'Manage KYC, KYB, AML screening, due diligence, monitoring, case investigation, and regulatory reporting workflows.';
