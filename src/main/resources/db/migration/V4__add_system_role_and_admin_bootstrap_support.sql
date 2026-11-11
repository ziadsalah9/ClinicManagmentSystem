-- System-level role for admin-provisioned accounts. Clinic-scoped roles remain in staff_membership.
ALTER TABLE identity.app_user
    ADD COLUMN IF NOT EXISTS system_role VARCHAR(16) NOT NULL DEFAULT 'USER';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_identity_user_system_role'
    ) THEN
        ALTER TABLE identity.app_user
            ADD CONSTRAINT ck_identity_user_system_role CHECK (system_role IN ('USER', 'ADMIN'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS ix_identity_user_system_role ON identity.app_user (system_role);