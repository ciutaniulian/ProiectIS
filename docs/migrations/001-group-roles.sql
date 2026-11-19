ALTER TABLE group_members ADD COLUMN role VARCHAR(16) NULL;

UPDATE group_members AS member
JOIN expense_groups AS expense_group ON expense_group.id = member.group_id
SET member.role = CASE
    WHEN expense_group.created_by = member.user_id THEN 'ADMIN'
    ELSE 'MEMBER'
END
WHERE member.role IS NULL;

ALTER TABLE group_members MODIFY COLUMN role VARCHAR(16) NOT NULL;
