-- V7: reconcile doctor onboarding invite to lightweight invite model (V6 was applied with heavier profile columns)
ALTER TABLE identity.doctor_onboarding_invite
    ALTER COLUMN professional_title DROP NOT NULL,
    ALTER COLUMN years_experience DROP NOT NULL,
    ALTER COLUMN gender DROP NOT NULL,
    ALTER COLUMN specialty_ids DROP NOT NULL;

ALTER TABLE identity.doctor_onboarding_invite
    DROP CONSTRAINT IF EXISTS ck_identity_doc_invite_gender;
ALTER TABLE identity.doctor_onboarding_invite
    ADD CONSTRAINT ck_identity_doc_invite_gender CHECK (gender IS NULL OR gender IN ('MALE','FEMALE','UNSPECIFIED'));
