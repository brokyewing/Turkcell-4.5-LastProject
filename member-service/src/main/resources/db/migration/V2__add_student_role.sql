-- member-service V2 — Student.role (STUDENT/LIBRARIAN) kolonunu ekler.
-- A4a entity'ye role ekledi, A4c V1__baseline.sql'i canli semadan (role yokken) uretti;
-- bu migration iki karti birlestirir. Idempotent: kolon/constraint zaten varsa atlar,
-- boylece hem dolu (V1 baseline) hem bos (V1 -> V2) DB'de calisir.

ALTER TABLE students ADD COLUMN IF NOT EXISTS role varchar(255);
UPDATE students SET role = 'STUDENT' WHERE role IS NULL;
ALTER TABLE students ALTER COLUMN role SET NOT NULL;
ALTER TABLE students ALTER COLUMN role SET DEFAULT 'STUDENT';

DO $$
BEGIN
  ALTER TABLE students ADD CONSTRAINT ck_students_role CHECK (role IN ('STUDENT','LIBRARIAN'));
EXCEPTION WHEN duplicate_object THEN NULL;
END $$;
