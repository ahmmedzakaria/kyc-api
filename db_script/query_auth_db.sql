-- User and User Roles
SELECT
    au.username,
    au.id AS user_id,
    roles.roles,
    roles.role_ids,
    scopes.scope
FROM auth_users au

         LEFT JOIN LATERAL (
    SELECT
        STRING_AGG(DISTINCT ar.name, ', ' ORDER BY ar.name) AS roles,
        STRING_AGG(DISTINCT ar.id::text, ', ' ORDER BY ar.id::text) AS role_ids
    FROM auth_user_roles aur
             INNER JOIN auth_roles ar
                        ON ar.id = aur.role_id
    WHERE aur.user_id = au.id
        ) roles ON TRUE

         LEFT JOIN LATERAL (
    SELECT
        STRING_AGG(
                CONCAT(
                        'tenant_id: ', ausa.tenant_id,
                        ', business_id: ', ausa.business_id,
                        ', branch_id: ', ausa.branch_id
                ),
                ' | '
        ) AS scope
    FROM auth_user_scope_assignments ausa
    WHERE ausa.user_id = au.id
        ) scopes ON TRUE;


select * from auth_roles;
select * from auth_user_roles;
select * from auth_client_auth_policy;
select * from auth_user_scope_assignments;
