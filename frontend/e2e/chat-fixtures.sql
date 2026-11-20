-- Seed only the disposable Playwright database.
INSERT INTO users (name, email, password_hash, role) VALUES
    ('First Chat User', 'first-chat@example.com', '$2a$10$kW8lsCMGQzQ8f5HMUaZ1ru7bJ5LFQScDWbd2JXPv.AU6Fx5VFCeKi', 'USER'),
    ('Second Chat User', 'second-chat@example.com', '$2a$10$kW8lsCMGQzQ8f5HMUaZ1ru7bJ5LFQScDWbd2JXPv.AU6Fx5VFCeKi', 'USER');

INSERT INTO group_memberships (group_id, user_id)
SELECT 1, id FROM users WHERE email IN ('first-chat@example.com', 'second-chat@example.com');

INSERT INTO group_memberships (group_id, user_id)
SELECT 2, id FROM users WHERE email = 'first-chat@example.com';

INSERT INTO expense_groups (id, name) VALUES (900, 'Legacy group');
INSERT INTO group_memberships (group_id, user_id)
SELECT 900, id FROM users WHERE email = 'first-chat@example.com';
INSERT INTO group_memberships (group_id, user_id)
SELECT 900, id FROM users WHERE email = 'second-chat@example.com';

INSERT INTO group_messages (group_id, sender_id, message, created_at)
SELECT 1,
    (SELECT id FROM users WHERE email = 'first-chat@example.com'),
    CONCAT('Legacy history ', X),
    DATEADD('SECOND', X, TIMESTAMP '2025-01-01 12:00:00')
FROM SYSTEM_RANGE(1, 55)
ORDER BY X;
