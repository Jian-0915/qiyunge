-- 听雨轩第二轮：歌曲格式识别
-- V2__add_song_format.sql

ALTER TABLE songs ADD COLUMN format TEXT;
ALTER TABLE songs ADD COLUMN codec TEXT;
