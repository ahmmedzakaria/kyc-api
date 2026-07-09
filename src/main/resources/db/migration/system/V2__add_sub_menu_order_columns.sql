-- Add backend-controlled sidebar ordering for existing system databases.

ALTER TABLE sys_sub_menus
    ADD COLUMN IF NOT EXISTS menu_order integer NOT NULL DEFAULT 0;

ALTER TABLE sys_sub_menus
    ADD COLUMN IF NOT EXISTS sub_menu_order integer NOT NULL DEFAULT 0;
