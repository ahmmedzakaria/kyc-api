-- Feature T-codes 001-100 are reserved for system quick navigation. Allocate
-- administrator-created feature codes from T101 upward without race-prone
-- MAX+1 application logic.

CREATE SEQUENCE IF NOT EXISTS sys_layout_t_code_seq
    AS bigint
    START WITH 101
    INCREMENT BY 1
    MINVALUE 101
    NO CYCLE;

SELECT setval(
    'sys_layout_t_code_seq',
    COALESCE((
        SELECT MAX(SUBSTRING(t_code FROM 2)::bigint)
        FROM sys_layout_features
        WHERE t_code ~ '^T[0-9]+$'
          AND SUBSTRING(t_code FROM 2)::bigint >= 101
    ), 101),
    EXISTS (
        SELECT 1 FROM sys_layout_features
        WHERE t_code ~ '^T[0-9]+$'
          AND SUBSTRING(t_code FROM 2)::bigint >= 101
    )
);
