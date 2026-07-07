package com.qiyunge.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiyunge.domain.entity.GalleryImage;
import com.qiyunge.infrastructure.util.StringUtils;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Pexels 图片源提供者。
 * API 和图片均直连访问（国内可直连 Cloudflare CDN）。
 */
public class PexelsProvider extends AbstractImageProvider {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String apiKey;

    // 默认 API Key（建议通过环境变量 PEXELS_API_KEY 或配置项覆盖）
    private static final String FALLBACK_API_KEY = "qzSEHGI0Z4Z9CYfAUgdkxW1lXxS3ft0iABTJ8rQHK52fhzjlbMYYPXyZ";

    public PexelsProvider() {
        this(null);
    }

    public PexelsProvider(String apiKey) {
        String resolved = StringUtils.firstNonBlank(
            apiKey,
            System.getProperty("qiyunge.pexels.apiKey"),
            System.getenv("PEXELS_API_KEY")
        );
        if (resolved == null || resolved.isBlank()) {
            resolved = FALLBACK_API_KEY;
            System.out.println("[PexelsProvider] 使用内置 API Key（建议通过环境变量 PEXELS_API_KEY 覆盖）");
        }
        this.apiKey = resolved;
    }

    @Override
    public String getProviderId() { return "pexels"; }

    @Override
    public String getProviderName() { return "Pexels"; }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    protected String buildSearchUrl(String keyword, int page, int pageSize) {
        String encoded = java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
        return "https://api.pexels.com/v1/search?query=" + encoded
            + "&per_page=" + pageSize + "&page=" + page + "&locale=zh-CN";
    }

    @Override
    protected void customizeRequest(HttpRequest.Builder builder) {
        builder.header("Authorization", apiKey);
    }

    @Override
    protected List<GalleryImage> parseResults(String json) {
        List<GalleryImage> list = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode photos = root.get("photos");
            if (photos == null || !photos.isArray()) return list;

            for (JsonNode photo : photos) {
                GalleryImage img = new GalleryImage();
                img.setSource("pexels");

                // 标题
                String alt = photo.path("alt").asText("");
                if (!alt.isEmpty()) {
                    img.setTitle(alt.length() > 50 ? alt.substring(0, 50) : alt);
                } else {
                    img.setTitle("Pexels图片");
                }

                // 图片URL：直接使用原始URL（国内可直连 Cloudflare CDN）
                JsonNode src = photo.path("src");
                String tiny = src.path("tiny").asText(null);
                String small = src.path("small").asText(null);
                String medium = src.path("medium").asText(null);
                String large = src.path("large").asText(null);

                // 缩略图优先级：medium > small > tiny，避免 tiny 变体在部分环境下加载为空。
                if (medium != null) img.setThumbnailUrl(medium);
                else if (small != null) img.setThumbnailUrl(small);
                else if (tiny != null) img.setThumbnailUrl(tiny);

                // 原图优先级：large > medium > small
                if (large != null) img.setUrl(large);
                else if (medium != null) img.setUrl(medium);
                else if (small != null) img.setUrl(small);

                // 宽高
                int width = photo.path("width").asInt(0);
                int height = photo.path("height").asInt(0);
                if (width > 0) img.setWidth(width);
                if (height > 0) img.setHeight(height);

                // 摄影师信息
                String photographer = photo.path("photographer").asText("");
                if (!photographer.isEmpty()) {
                    img.setSubCategory("by " + photographer);
                }

                list.add(img);
            }
        } catch (Exception e) {
            System.err.println("[PexelsProvider] 解析失败: " + e.getMessage());
        }
        return list;
    }

}
