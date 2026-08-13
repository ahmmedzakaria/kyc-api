ALTER TABLE sys_acc_api_registry ADD COLUMN IF NOT EXISTS version bigint NOT NULL DEFAULT 0;
