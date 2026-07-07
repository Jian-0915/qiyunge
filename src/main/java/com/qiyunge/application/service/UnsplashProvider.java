package com.qiyunge.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiyunge.domain.entity.GalleryImage;
import com.qiyunge.infrastructure.util.StringUtils;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Unsplash 图片源提供者。
 * API 直连访问，图片 URL 直接使用原始地址（不走代理）。
 */
public class UnsplashProvider extends AbstractImageProvider {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String clientId;

    public UnsplashProvider() {
        this(null);
    }

    public UnsplashProvider(String clientId) {
        this.clientId = StringUtils.firstNonBlank(
            clientId,
            System.getProperty("qiyunge.unsplash.clientId"),
            System.getenv("UNSPLASH_CLIENT_ID")
        );
    }

    @Override
    public String getProviderId() { return "unsplash"; }

    @Override
    public String getProviderName() { return "Unsplash"; }

    @Override
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank();
    }

    @Override
    protected String buildSearchUrl(String keyword, int page, int pageSize) {
        String encoded = java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
        return "https://api.unsplash.com/search/photos?query=" + encoded
            + "&per_page=" + pageSize + "&page=" + page;
    }

    @Override
    protected void customizeRequest(HttpRequest.Builder builder) {
        builder.header("Authorization", "Client-ID " + clientId);
    }

    @Override
    protected List<GalleryImage> parseResults(String json) {
        List<GalleryImage> list = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode results = root.get("results");
            if (results == null || !results.isArray()) return list;

            for (JsonNode item : results) {
                GalleryImage img = new GalleryImage();
                img.setSource("unsplash");

                // 提取描述作为标题
                String desc = item.path("description").asText(null);
                if (desc == null || desc.isEmpty() || desc.equals("null")) {
                    desc = item.path("alt_description").asText(null);
                }
                if (desc != null && !desc.isEmpty() && !desc.equals("null")) {
                    img.setTitle(desc.length() > 50 ? desc.substring(0, 50) : desc);
                } else {
                    img.setTitle("Unsplash图片");
                }

                // 图片URL：直接使用原始URL（不走wsrv.nl代理）
                JsonNode urls = item.path("urls");
                String regular = urls.path("regular").asText(null);
                String thumb = urls.path("thumb").asText(null);

                if (thumb != null) img.setThumbnailUrl(thumb);
                if (regular != null) img.setUrl(regular);

                // 宽高
                int width = item.path("width").asInt(0);
                int height = item.path("height").asInt(0);
                if (width > 0) img.setWidth(width);
                if (height > 0) img.setHeight(height);

                // 摄影师信息
                String user = item.path("user").path("name").asText("");
                if (!user.isEmpty()) {
                    img.setSubCategory("by " + user);
                }

                list.add(img);
            }
        } catch (Exception e) {
            System.err.println("[UnsplashProvider] 解析失败: " + e.getMessage());
        }
        return list;
    }

}
