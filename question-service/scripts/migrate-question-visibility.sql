ALTER TABLE question ADD COLUMN IF NOT EXISTS visibility VARCHAR(32);

UPDATE question
SET visibility = status
WHERE visibility IS NULL
  AND status IN ('PUBLIC', 'PRIVATE');

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM question
        WHERE visibility IS NULL
           OR visibility NOT IN ('PUBLIC', 'PRIVATE')
    ) THEN
        RAISE EXCEPTION 'Question visibility migration found invalid legacy rows';
    END IF;
END $$;

UPDATE question
SET status = 'ACTIVE'
WHERE status IN ('PUBLIC', 'PRIVATE');

ALTER TABLE question ALTER COLUMN visibility SET NOT NULL;
