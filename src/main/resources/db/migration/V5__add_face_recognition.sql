-- 用户人脸数据表
CREATE TABLE IF NOT EXISTS user_face_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL UNIQUE,
    model_path TEXT,
    face_image_path TEXT,
    sample_count INTEGER DEFAULT 0,
    enabled INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_face_user_id ON user_face_data(user_id);
