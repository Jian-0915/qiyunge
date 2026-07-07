-- 图集相关表
CREATE TABLE image_albums (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    cover_image_id INTEGER,
    created_at TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (cover_image_id) REFERENCES gallery_images(id)
);

CREATE TABLE image_album_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    album_id INTEGER NOT NULL,
    image_id INTEGER NOT NULL,
    added_at TEXT DEFAULT (datetime('now')),
    FOREIGN KEY (album_id) REFERENCES image_albums(id) ON DELETE CASCADE,
    FOREIGN KEY (image_id) REFERENCES gallery_images(id) ON DELETE CASCADE,
    UNIQUE(album_id, image_id)
);

CREATE INDEX idx_album_items_album ON image_album_items(album_id);
