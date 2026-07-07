package com.qiyunge.domain.entity;

import java.time.Instant;

/**
 * 可播放音源：封装播放地址、格式、有效期和来源标识。
 */
public class PlayableSource {
    private String url;
    private String format;
    private String codec;
    private Instant expireAt;
    private String provider;

    public PlayableSource() {}

    public PlayableSource(String url, String format, String provider) {
        this.url = url;
        this.format = format;
        this.provider = provider;
    }

    // Getters / Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public String getCodec() { return codec; }
    public void setCodec(String codec) { this.codec = codec; }
    public Instant getExpireAt() { return expireAt; }
    public void setExpireAt(Instant expireAt) { this.expireAt = expireAt; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    /** 检查播放地址是否已过期 */
    public boolean isExpired() {
        return expireAt != null && Instant.now().isAfter(expireAt);
    }
}
