-- ============================================================
-- 专注计时（Pomodoro）模块表
-- QiyunGe Database Schema V9
-- ============================================================

-- 扩展 pomodoro_sessions 表：添加任务关联、详细时间戳、完成状态、标签
CREATE TABLE IF NOT EXISTS pomodoro_sessions_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    task_id INTEGER,
    duration_minutes INTEGER NOT NULL,
    session_type TEXT NOT NULL DEFAULT 'focus',
    session_date TEXT NOT NULL,
    start_time TEXT,
    end_time TEXT,
    is_completed INTEGER NOT NULL DEFAULT 1,
    tag TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT INTO pomodoro_sessions_new (id, user_id, duration_minutes, session_date, created_at)
SELECT id, user_id, duration_minutes, session_date, created_at FROM pomodoro_sessions;

DROP TABLE IF EXISTS pomodoro_sessions;
ALTER TABLE pomodoro_sessions_new RENAME TO pomodoro_sessions;

-- 番茄钟任务表
CREATE TABLE IF NOT EXISTS pomodoro_tasks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    estimated_pomodoros INTEGER NOT NULL DEFAULT 1,
    completed_pomodoros INTEGER NOT NULL DEFAULT 0,
    tag TEXT,
    is_completed INTEGER NOT NULL DEFAULT 0,
    sort_order INTEGER NOT NULL DEFAULT 0,
    task_date TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 任务模板表
CREATE TABLE IF NOT EXISTS pomodoro_task_templates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    title TEXT NOT NULL,
    estimated_pomodoros INTEGER NOT NULL DEFAULT 1,
    tag TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, title)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_pomodoro_sessions_user_date ON pomodoro_sessions(user_id, session_date);
CREATE INDEX IF NOT EXISTS idx_pomodoro_sessions_user_task ON pomodoro_sessions(user_id, task_id);
CREATE INDEX IF NOT EXISTS idx_pomodoro_tasks_user_date ON pomodoro_tasks(user_id, task_date, sort_order);
CREATE INDEX IF NOT EXISTS idx_pomodoro_templates_user ON pomodoro_task_templates(user_id);
