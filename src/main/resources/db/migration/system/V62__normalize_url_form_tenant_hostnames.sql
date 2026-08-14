-- Tenant-domain identity is a hostname, never an origin or URL. Older UI flows
-- could persist a full URL, which cannot match HttpServletRequest#getServerName.
WITH candidates AS (
    SELECT id,
           lower(regexp_replace(
               split_part(regexp_replace(hostname, '^https?://', '', 'i'), '/', 1),
               ':[0-9]+$', '')) AS normalized_hostname
    FROM sys_tenant_domains
    WHERE hostname ~* '^https?://'
), safe_candidates AS (
    SELECT candidate.*
    FROM candidates candidate
    WHERE candidate.normalized_hostname <> ''
      AND NOT EXISTS (
          SELECT 1
          FROM sys_tenant_domains existing
          WHERE existing.id <> candidate.id
            AND lower(existing.hostname) = candidate.normalized_hostname
      )
)
UPDATE sys_tenant_domains domain
SET hostname = candidate.normalized_hostname,
    updated_at = CURRENT_TIMESTAMP
FROM safe_candidates candidate
WHERE domain.id = candidate.id;
