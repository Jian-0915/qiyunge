package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.GalleryImage;
import com.qiyunge.infrastructure.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 图集图片关联数据访问层。
 */
public class ImageAlbumItemRepository {

    private final DatabaseManager dbManager;

    public ImageAlbumItemRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /** 添加图片到图集 */
    public boolean addItem(int albumId, int imageId) {
        return dbManager.withConnection(conn -> {
            String sql = "INSERT OR IGNORE INTO image_album_items (album_id, image_id) VALUES (?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, albumId);
                stmt.setInt(2, imageId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    /** 从图集移除图片 */
    public boolean removeItem(int albumId, int imageId) {
        return dbManager.withConnection(conn -> {
            String sql = "DELETE FROM image_album_items WHERE album_id = ? AND image_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, albumId);
                stmt.setInt(2, imageId);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    /** 查询图集中的图片列表 */
    public List<GalleryImage> findImagesByAlbumId(int albumId) {
        return dbManager.withConnection(conn -> {
            List<GalleryImage> list = new ArrayList<>();
            String sql = "SELECT gi.* FROM gallery_images gi " +
                "INNER JOIN image_album_items ai ON gi.id = ai.image_id " +
                "WHERE ai.album_id = ? ORDER BY ai.added_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, albumId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapImage(rs));
                    }
                }
            }
            return list;
        });
    }

    /** 查询图集中的图片 ID 列表 */
    public List<Integer> findImageIdsByAlbumId(int albumId) {
        return dbManager.withConnection(conn -> {
            List<Integer> ids = new ArrayList<>();
            String sql = "SELECT image_id FROM image_album_items WHERE album_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, albumId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getInt(1));
                    }
                }
            }
            return ids;
        });
    }

    /** 统计图集中的图片数量 */
    public int countByAlbum(int albumId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM image_album_items WHERE album_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, albumId);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }

    /** 判断图片是否在图集中 */
    public boolean isInAlbum(int albumId, int imageId) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT 1 FROM image_album_items WHERE album_id = ? AND image_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, albumId);
                stmt.setInt(2, imageId);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next();
                }
            }
        });
    }

    /** 查询图片所属的所有图集 ID */
    public List<Integer> findAlbumIdsByImageId(int imageId) {
        return dbManager.withConnection(conn -> {
            List<Integer> ids = new ArrayList<>();
            String sql = "SELECT album_id FROM image_album_items WHERE image_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, imageId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ids.add(rs.getInt(1));
                    }
                }
            }
            return ids;
        });
    }

    /** 删除某图片在所有图集中的关联记录（图片被删除时调用） */
    public void removeAllByImageId(int imageId) {
        dbManager.withConnection(conn -> {
            String sql = "DELETE FROM image_album_items WHERE image_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, imageId);
                stmt.executeUpdate();
            }
            return null;
        });
    }

    /** 结果集映射 */
    private GalleryImage mapImage(ResultSet rs) throws java.sql.SQLException {
        return GalleryImageRepository.mapImage(rs);
    }
}
