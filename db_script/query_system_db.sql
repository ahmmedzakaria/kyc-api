-- Role wise features
SELECT
    au.username,
    STRING_AGG(ar.name, ', ' ORDER BY ar.name) AS roles,
    STRING_AGG(ar.id::text, ', ' ORDER BY ar.name) AS role_ids,
    MAX(au.id) AS user_id,
    MAX('tenant_id: ' || ausa.tenant_id || ', business_id: ' || ausa.business_id || ', branch_id: ' || ausa.branch_id) as scope
FROM auth_users au
         INNER JOIN auth_user_roles aur
                    ON au.id = aur.user_id
         INNER JOIN public.auth_roles ar
                    ON ar.id = aur.role_id
         Inner join public.auth_user_scope_assignments ausa on au.id = ausa.user_id
GROUP BY au.username;