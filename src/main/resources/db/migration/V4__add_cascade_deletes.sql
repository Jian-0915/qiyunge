-- V4: 为关键关联表添加 ON DELETE CASCADE，防止数据不一致

-- SQLite 不支持 ALTER TABLE 修改外键约束，需要重建表

-- 1. favorite_songs: 删除歌曲时级联删除收藏记录
CREATE TABLE IF NOT EXISTS favorite_songs_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    song_id INTEGER NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
    UNIQUE(user_id, song_id)
);
INSERT OR IGNORE INTO favorite_songs_new SELECT * FROM favorite_songs;
DROP TABLE favorite_songs;
ALTER TABLE favorite_songs_new RENAME TO favorite_songs;
CREATE INDEX IF NOT EXISTS idx_favorite_songs_user ON favorite_songs(user_id);

-- 2. play_history: 删除歌曲时级联删除播放历史
CREATE TABLE IF NOT EXISTS play_history_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    song_id INTEGER NOT NULL,
    played_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
);
INSERT OR IGNORE INTO play_history_new SELECT * FROM play_history;
DROP TABLE play_history;
ALTER TABLE play_history_new RENAME TO play_history;
CREATE INDEX IF NOT EXISTS idx_play_history_user ON play_history(user_id);
CREATE INDEX IF NOT EXISTS idx_play_history_played ON play_history(played_at);

-- 3. user_image_preferences: 删除图片时级联删除收藏记录
CREATE TABLE IF NOT EXISTS user_image_preferences_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    image_id INTEGER NOT NULL,
    preference INTEGER DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES gallery_images(id) ON DELETE CASCADE,
    UNIQUE(user_id, image_id)
);
INSERT OR IGNORE INTO user_image_preferences_new SELECT * FROM user_image_preferences;
DROP TABLE user_image_preferences;
ALTER TABLE user_image_preferences_new RENAME TO user_image_preferences;
CREATE INDEX IF NOT EXISTS idx_user_prefs_user ON user_image_preferences(user_id);

-- 4. playlists: 删除用户时级联删除歌单
CREATE TABLE IF NOT EXISTS playlists_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    cover_url TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
INSERT OR IGNORE INTO playlists_new SELECT * FROM playlists;
DROP TABLE playlists;
ALTER TABLE playlists_new RENAME TO playlists;
CREATE INDEX IF NOT EXISTS idx_playlists_user ON playlists(user_id);

-- 5. playlist_songs: 确保有 CASCADE（V1 中已有，但重建 playlists 后需要重建 playlist_songs）
CREATE TABLE IF NOT EXISTS playlist_songs_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    playlist_id INTEGER NOT NULL,
    song_id INTEGER NOT NULL,
    sort_order INTEGER DEFAULT 0,
    added_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
    UNIQUE(playlist_id, song_id)
);
INSERT OR IGNORE INTO playlist_songs_new SELECT * FROM playlist_songs;
DROP TABLE playlist_songs;
ALTER TABLE playlist_songs_new RENAME TO playlist_songs;

-- 6. 清理残留的孤儿数据（外键约束已无法通过的数据）
DELETE FROM favorite_songs WHERE song_id NOT IN (SELECT id FROM songs);
DELETE FROM play_history WHERE song_id NOT IN (SELECT id FROM songs);
DELETE FROM user_image_preferences WHERE image_id NOT IN (SELECT id FROM gallery_images);
DELETE FROM playlist_songs WHERE song_id NOT IN (SELECT id FROM songs);
DELETE FROM playlist_songs WHERE playlist_id NOT IN (SELECT id FROM playlists);
