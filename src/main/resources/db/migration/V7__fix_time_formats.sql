-- V7: 修复 V3 和 V5 中时间格式不一致的问题
-- V3 的 image_albums/image_album_items 使用了 datetime('now')（UTC时间）
-- V5 的 user_face_data 使用了 DATETIME DEFAULT CURRENT_TIMESTAMP（UTC时间）
-- 统一改为本地时间格式，与 V1/V4/V6 保持一致

-- 重建 user_face_data 表以修正时间格式
CREATE TABLE IF NOT EXISTS user_face_data_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL UNIQUE,
    model_path TEXT,
    face_image_path TEXT,
    sample_count INTEGER DEFAULT 0,
    enabled INTEGER DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

INSERT OR IGNORE INTO user_face_data_new (id, user_id, model_path, face_image_path, sample_count, enabled, created_at, updated_at)
    SELECT id, user_id, model_path, face_image_path, sample_count, enabled, created_at, updated_at FROM user_face_data;

DROP TABLE IF EXISTS user_face_data;
ALTER TABLE user_face_data_new RENAME TO user_face_data;

CREATE INDEX IF NOT EXISTS idx_user_face_user_id ON user_face_data(user_id);
