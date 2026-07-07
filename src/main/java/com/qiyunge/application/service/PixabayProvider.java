package com.qiyunge.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiyunge.domain.entity.GalleryImage;
import com.qiyunge.infrastructure.util.StringUtils;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Pixabay 图片源提供者。
 * Pixabay API 在国内网络环境下可正常访问，作为首选图源。
 */
public class PixabayProvider extends AbstractImageProvider {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final String apiKey;

    public PixabayProvider() {
        this(null);
    }

    public PixabayProvider(String apiKey) {
        this.apiKey = StringUtils.firstNonBlank(
            apiKey,
            System.getProperty("qiyunge.pixabay.apiKey"),
            System.getenv("PIXABAY_API_KEY")
        );
    }

    @Override
    public String getProviderId() { return "pixabay"; }

    @Override
    public String getProviderName() { return "Pixabay"; }

    @Override
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    @Override
    protected String buildSearchUrl(String keyword, int page, int pageSize) {
        String encoded = java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
        return "https://pixabay.com/api/?key=" + apiKey
            + "&q=" + encoded
            + "&per_page=" + pageSize
            + "&page=" + page
            + "&image_type=photo"
            + "&lang=zh";
    }

    @Override
    protected List<GalleryImage> parseResults(String json) {
        List<GalleryImage> list = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode hits = root.get("hits");
            if (hits == null || !hits.isArray()) return list;

            for (JsonNode hit : hits) {
                GalleryImage img = new GalleryImage();
                img.setSource("pixabay");

                // 标签作为标题
                String tags = hit.path("tags").asText("");
                if (!tags.isEmpty()) {
                    String firstTag = tags.split(",")[0].trim();
                    img.setTitle(firstTag.length() > 50 ? firstTag.substring(0, 50) : firstTag);
                } else {
                    img.setTitle("Pixabay图片");
                }

                // 图片URL：通过 wsrv.nl 代理加速（pixabay CDN 在国内偶有波动）
                String webformatURL = hit.path("webformatURL").asText(null);
                if (webformatURL != null) {
                    img.setUrl(proxyImageUrl(webformatURL, 940, 650));
                }

                // 缩略图URL
                String previewURL = hit.path("previewURL").asText(null);
                if (previewURL != null) {
                    img.setThumbnailUrl(proxyImageUrl(previewURL, 200, 130));
                }

                // 宽高
                int width = hit.path("webformatWidth").asInt(0);
                int height = hit.path("webformatHeight").asInt(0);
                if (width == 0) width = hit.path("imageWidth").asInt(0);
                if (height == 0) height = hit.path("imageHeight").asInt(0);
                if (width > 0) img.setWidth(width);
                if (height > 0) img.setHeight(height);

                // 文件大小
                long size = hit.path("imageSize").asLong(0);
                if (size > 0) img.setFileSize(size);

                // 摄影师信息
                String user = hit.path("user").asText("");
                if (!user.isEmpty()) {
                    img.setSubCategory("by " + user);
                }

                list.add(img);
            }
        } catch (Exception e) {
            System.err.println("[PixabayProvider] 解析失败: " + e.getMessage());
        }
        return list;
    }

    /**
     * 通过 wsrv.nl 图片代理服务转换URL，加速国内访问。
     */
    private String proxyImageUrl(String originalUrl, int width, int height) {
        try {
            String encoded = java.net.URLEncoder.encode(originalUrl, java.nio.charset.StandardCharsets.UTF_8);
            return "https://wsrv.nl/?url=" + encoded + "&w=" + width + "&h=" + height + "&fit=cover";
        } catch (Exception e) {
            return originalUrl;
        }
    }
}
