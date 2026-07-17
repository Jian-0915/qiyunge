-- 移除 registration_requests 表的 username UNIQUE 约束
-- 允许同一用户在申请被驳回/账号注销后重新提交注册申请

CREATE TABLE IF NOT EXISTS registration_requests_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    display_name TEXT,
    reason TEXT,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    reviewed_at TEXT,
    reviewed_by INTEGER,
    FOREIGN KEY (reviewed_by) REFERENCES users(id)
);

INSERT INTO registration_requests_new (id, username, password_hash, display_name, reason, status, created_at, reviewed_at, reviewed_by)
SELECT id, username, password_hash, display_name, reason, status, created_at, reviewed_at, reviewed_by
FROM registration_requests;

DROP TABLE registration_requests;

ALTER TABLE registration_requests_new RENAME TO registration_requests;

CREATE INDEX IF NOT EXISTS idx_registration_status ON registration_requests(status);
CREATE INDEX IF NOT EXISTS idx_registration_username ON registration_requests(username);
