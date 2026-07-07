package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.ImageAlbum;
import com.qiyunge.infrastructure.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 图集数据访问层。
 */
public class ImageAlbumRepository {

    private final DatabaseManager dbManager;

    public ImageAlbumRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /** 查询所有图集，附带图片数量 */
    public List<ImageAlbum> findAll() {
        return dbManager.withConnection(conn -> {
            List<ImageAlbum> list = new ArrayList<>();
            String sql = "SELECT a.*, COUNT(ai.id) AS image_count " +
                "FROM image_albums a LEFT JOIN image_album_items ai ON a.id = ai.album_id " +
                "GROUP BY a.id ORDER BY a.created_at DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapAlbum(rs));
                }
            }
            return list;
        });
    }

    /** 按 ID 查询 */
    public ImageAlbum findById(int id) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT a.*, COUNT(ai.id) AS image_count " +
                "FROM image_albums a LEFT JOIN image_album_items ai ON a.id = ai.album_id " +
                "WHERE a.id = ? GROUP BY a.id";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapAlbum(rs);
                    }
                }
            }
            return null;
        });
    }

    /** 新建图集，返回带自增 ID 的对象 */
    public ImageAlbum create(ImageAlbum album) {
        return dbManager.withConnection(conn -> {
            String sql = "INSERT INTO image_albums (name, description, cover_image_id) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, album.getName());
                stmt.setString(2, album.getDescription());
                if (album.getCoverImageId() > 0) {
                    stmt.setInt(3, album.getCoverImageId());
                } else {
                    stmt.setNull(3, java.sql.Types.INTEGER);
                }
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        album.setId(keys.getInt(1));
                    }
                }
                return album;
            }
        });
    }

    /** 更新图集信息 */
    public boolean update(ImageAlbum album) {
        return dbManager.withConnection(conn -> {
            String sql = "UPDATE image_albums SET name = ?, description = ?, cover_image_id = ? WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, album.getName());
                stmt.setString(2, album.getDescription());
                if (album.getCoverImageId() > 0) {
                    stmt.setInt(3, album.getCoverImageId());
                } else {
                    stmt.setNull(3, java.sql.Types.INTEGER);
                }
                stmt.setInt(4, album.getId());
                return stmt.executeUpdate() > 0;
            }
        });
    }

    /** 删除图集（级联删除关联项） */
    public boolean delete(int id) {
        return dbManager.withConnection(conn -> {
            try (PreparedStatement delItems = conn.prepareStatement("DELETE FROM image_album_items WHERE album_id = ?")) {
                delItems.setInt(1, id);
                delItems.executeUpdate();
            }
            String sql = "DELETE FROM image_albums WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    /** 统计图集总数 */
    public int countAll() {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM image_albums";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    /** 结果集映射 */
    private ImageAlbum mapAlbum(ResultSet rs) throws java.sql.SQLException {
        ImageAlbum album = new ImageAlbum();
        album.setId(rs.getInt("id"));
        album.setName(rs.getString("name"));
        album.setDescription(rs.getString("description"));
        album.setCoverImageId(rs.getInt("cover_image_id"));
        album.setCreatedAt(rs.getString("created_at"));
        album.setImageCount(rs.getInt("image_count"));
        return album;
    }
}
