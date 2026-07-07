package com.qiyunge.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qiyunge.domain.entity.GalleryImage;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * Wikimedia Commons 图片源提供者。
 * 不需要 API Key，作为在线寻图的默认兜底图源。
 */
public class WikimediaProvider extends AbstractImageProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String getProviderId() { return "wikimedia"; }

    @Override
    public String getProviderName() { return "Wikimedia"; }

    @Override
    protected String buildSearchUrl(String keyword, int page, int pageSize) {
        String encoded = java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8);
        int offset = Math.max(page - 1, 0) * pageSize;
        return "https://commons.wikimedia.org/w/api.php"
            + "?action=query"
            + "&generator=search"
            + "&gsrnamespace=6"
            + "&gsrsearch=" + encoded
            + "&gsrlimit=" + pageSize
            + "&gsroffset=" + offset
            + "&prop=imageinfo"
            + "&iiprop=url%7Cmime%7Csize%7Cextmetadata"
            + "&iiurlwidth=600"
            + "&iiurlheight=420"
            + "&format=json"
            + "&formatversion=2"
            + "&origin=*";
    }

    @Override
    protected void customizeRequest(HttpRequest.Builder builder) {
        builder.header("User-Agent", "QiyunGe/1.0 (desktop online image search)");
    }

    @Override
    protected List<GalleryImage> parseResults(String json) {
        List<GalleryImage> list = new ArrayList<>();
        try {
            JsonNode pages = MAPPER.readTree(json).path("query").path("pages");
            if (!pages.isArray()) return list;

            for (JsonNode page : pages) {
                JsonNode imageInfo = firstImageInfo(page.path("imageinfo"));
                if (imageInfo == null || !isSupportedImage(imageInfo.path("mime").asText(""))) {
                    continue;
                }

                String url = imageInfo.path("url").asText(null);
                String thumbUrl = imageInfo.path("thumburl").asText(url);
                if (url == null || url.isBlank()) {
                    continue;
                }

                GalleryImage img = new GalleryImage();
                img.setSource("wikimedia");
                img.setTitle(extractTitle(page, imageInfo));
                img.setUrl(url);
                img.setThumbnailUrl(thumbUrl);
                img.setWidth(imageInfo.path("width").asInt(0));
                img.setHeight(imageInfo.path("height").asInt(0));
                img.setFileSize(imageInfo.path("size").asLong(0));

                String artist = cleanMetadata(imageInfo.path("extmetadata").path("Artist").path("value").asText(""));
                if (!artist.isBlank()) {
                    img.setSubCategory("by " + artist);
                }
                list.add(img);
            }
        } catch (Exception e) {
            System.err.println("[WikimediaProvider] 解析失败: " + e.getMessage());
        }
        return list;
    }

    private JsonNode firstImageInfo(JsonNode imageInfoArray) {
        if (!imageInfoArray.isArray() || imageInfoArray.isEmpty()) {
            return null;
        }
        return imageInfoArray.get(0);
    }

    private boolean isSupportedImage(String mime) {
        return "image/jpeg".equals(mime)
            || "image/png".equals(mime)
            || "image/gif".equals(mime)
            || "image/webp".equals(mime);
    }

    private String extractTitle(JsonNode page, JsonNode imageInfo) {
        String description = cleanMetadata(imageInfo.path("extmetadata").path("ImageDescription").path("value").asText(""));
        if (!description.isBlank()) {
            return trimTitle(description);
        }

        String title = page.path("title").asText("Wikimedia 图片");
        if (title.startsWith("File:")) {
            title = title.substring("File:".length());
        }
        int dot = title.lastIndexOf('.');
        if (dot > 0) {
            title = title.substring(0, dot);
        }
        return trimTitle(title.replace('_', ' '));
    }

    private String trimTitle(String title) {
        String clean = title.strip();
        return clean.length() > 50 ? clean.substring(0, 50) : clean;
    }

    private String cleanMetadata(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value
            .replaceAll("<[^>]+>", "")
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .strip();
    }
}
