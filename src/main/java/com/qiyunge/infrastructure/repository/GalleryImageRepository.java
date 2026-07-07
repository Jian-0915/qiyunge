package com.qiyunge.infrastructure.repository;

import com.qiyunge.domain.entity.GalleryImage;
import com.qiyunge.infrastructure.database.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 图库图片数据访问层。
 */
public class GalleryImageRepository {

    private final DatabaseManager dbManager;

    public GalleryImageRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /** 查询所有图片，按创建时间倒序 */
    public List<GalleryImage> findAll() {
        return dbManager.withConnection(conn -> {
            List<GalleryImage> list = new ArrayList<>();
            String sql = "SELECT * FROM gallery_images ORDER BY created_at DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(mapImage(rs));
                }
            }
            return list;
        });
    }

    /** 按分类查询 */
    public List<GalleryImage> findByCategory(String category) {
        return dbManager.withConnection(conn -> {
            List<GalleryImage> list = new ArrayList<>();
            String sql = "SELECT * FROM gallery_images WHERE category = ? ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, category);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapImage(rs));
                    }
                }
            }
            return list;
        });
    }

    /** 按 ID 查询 */
    public GalleryImage findById(int id) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT * FROM gallery_images WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapImage(rs);
                    }
                }
            }
            return null;
        });
    }

    /** 搜索（标题或分类模糊匹配） */
    public List<GalleryImage> search(String keyword) {
        return dbManager.withConnection(conn -> {
            List<GalleryImage> list = new ArrayList<>();
            String sql = "SELECT * FROM gallery_images WHERE title LIKE ? OR category LIKE ? ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                String pattern = "%" + keyword + "%";
                stmt.setString(1, pattern);
                stmt.setString(2, pattern);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapImage(rs));
                    }
                }
            }
            return list;
        });
    }

    /** 新增图片，返回自增 ID */
    public GalleryImage create(GalleryImage image) {
        return dbManager.withConnection(conn -> {
            String sql = "INSERT INTO gallery_images (title, category, sub_category, url, local_path, " +
                "thumbnail_url, width, height, file_size, source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setString(1, image.getTitle());
                stmt.setString(2, image.getCategory());
                stmt.setString(3, image.getSubCategory());
                stmt.setString(4, image.getUrl());
                stmt.setString(5, image.getLocalPath());
                stmt.setString(6, image.getThumbnailUrl());
                stmt.setInt(7, image.getWidth());
                stmt.setInt(8, image.getHeight());
                stmt.setLong(9, image.getFileSize());
                stmt.setString(10, image.getSource() != null ? image.getSource() : "local");
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        image.setId(keys.getInt(1));
                    }
                }
                return image;
            }
        });
    }

    /** 删除图片 */
    public boolean delete(int id) {
        return dbManager.withConnection(conn -> {
            String sql = "DELETE FROM gallery_images WHERE id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                return stmt.executeUpdate() > 0;
            }
        });
    }

    /** 批量删除图片 */
    public boolean deleteByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return false;
        return dbManager.withConnection(conn -> {
            String placeholders = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
            String sql = "DELETE FROM gallery_images WHERE id IN (" + placeholders + ")";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < ids.size(); i++) {
                    stmt.setInt(i + 1, ids.get(i));
                }
                return stmt.executeUpdate() > 0;
            }
        });
    }

    /** 按 ID 列表查询 */
    public List<GalleryImage> findByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();
        return dbManager.withConnection(conn -> {
            List<GalleryImage> list = new ArrayList<>();
            String placeholders = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
            String sql = "SELECT * FROM gallery_images WHERE id IN (" + placeholders + ") ORDER BY created_at DESC";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                for (int i = 0; i < ids.size(); i++) {
                    stmt.setInt(i + 1, ids.get(i));
                }
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    list.add(mapImage(rs));
                }
            }
            return list;
        });
    }

    /** 统计总数 */
    public int countAll() {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM gallery_images";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        });
    }

    /** 按分类统计 */
    public int countByCategory(String category) {
        return dbManager.withConnection(conn -> {
            String sql = "SELECT COUNT(*) FROM gallery_images WHERE category = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, category);
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }

    /** 结果集映射 */
    public static GalleryImage mapImage(ResultSet rs) throws java.sql.SQLException {
        GalleryImage image = new GalleryImage();
        image.setId(rs.getInt("id"));
        image.setTitle(rs.getString("title"));
        String category = rs.getString("category");
        image.setCategory(category != null && !category.isBlank() && !"在线".equals(category) ? category : "其他");
        image.setSubCategory(rs.getString("sub_category"));
        image.setUrl(rs.getString("url"));
        image.setLocalPath(rs.getString("local_path"));
        image.setThumbnailUrl(rs.getString("thumbnail_url"));
        image.setWidth(rs.getInt("width"));
        image.setHeight(rs.getInt("height"));
        image.setFileSize(rs.getLong("file_size"));
        image.setSource(rs.getString("source"));
        image.setCreatedAt(rs.getString("created_at"));
        return image;
    }
}
