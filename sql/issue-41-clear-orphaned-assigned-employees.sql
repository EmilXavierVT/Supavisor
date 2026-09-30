-- Issue #41: assignments.assigned_employee_id becomes a foreign key to users(id).
--
-- hbm2ddl.auto = update adds the constraint on the next startup, but PostgreSQL refuses it
-- if any assignment still points at a user id that no longer exists. The application still
-- starts in that case: Hibernate only logs a WARN ("Error executing DDL ... add constraint")
-- and the foreign key is silently missing until the rows are cleared and the app is restarted.
--
-- Run this by hand against a database BEFORE the new version is deployed to it.
-- It is not run automatically.

-- 1. Find the orphaned references (look at these first).
SELECT a.id, a.tenant_id, a.name, a.assigned_employee_id
FROM assignments a
LEFT JOIN users u ON u.id = a.assigned_employee_id
WHERE a.assigned_employee_id IS NOT NULL
  AND u.id IS NULL
ORDER BY a.tenant_id, a.id;

-- 2. Clear them. The assignments stay, they just have no responsible person afterwards.
UPDATE assignments a
SET assigned_employee_id = NULL
WHERE a.assigned_employee_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.id = a.assigned_employee_id);

-- 3. After the new version has started, check that the constraint really was created.
SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'assignments'::regclass
  AND contype = 'f';
