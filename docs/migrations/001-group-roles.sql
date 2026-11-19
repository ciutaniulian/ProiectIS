ALTER TABLE group_members ADD COLUMN role VARCHAR(16) NULL;

UPDATE group_members AS gm
JOIN expense_groups AS eg ON eg.id = gm.group_id
SET gm.role = CASE
    WHEN eg.created_by = gm.user_id THEN 'ADMIN'
    ELSE 'MEMBER'
END
WHERE gm.role IS NULL;

ALTER TABLE group_members MODIFY COLUMN role VARCHAR(16) NOT NULL;
