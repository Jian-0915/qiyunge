package com.qiyunge.application.service;

import com.qiyunge.domain.entity.GalleryImage;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 图片源提供者基类。
 * 封装代理配置、超时重试等公共逻辑。
 *
 * 连接策略：默认直连；如用户设置 HTTP_PROXY / HTTPS_PROXY，则使用环境代理。
 * 图片 URL 由 JavaFX Image 直接加载（images.pexels.com 国内可直连）。
 */
public abstract class AbstractImageProvider implements ImageProvider {

    private HttpClient httpClient;

    /** 当前代理的认证令牌 */
    private volatile String proxyAuthToken;

    private static final int MAX_RETRIES = 1;
    private static final long RETRY_DELAY_MS = 500;

    protected AbstractImageProvider() {
        buildHttpClient();
    }

    private void buildHttpClient() {
        var builder = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL);

        this.proxyAuthToken = null;

        // 优先使用环境变量代理
        String envProxy = System.getenv("HTTP_PROXY");
        if (envProxy == null) envProxy = System.getenv("http_proxy");
        if (envProxy == null) envProxy = System.getenv("HTTPS_PROXY");
        if (envProxy == null) envProxy = System.getenv("https_proxy");

        if (envProxy != null && !envProxy.isEmpty()) {
            applyProxyConfig(builder, envProxy);
        } else {
            System.out.println("[" + getProviderId() + "] 直连模式（不走代理）");
        }

        this.httpClient = builder.build();
    }

    private void applyProxyConfig(HttpClient.Builder builder, String proxyStr) {
        try {
            String[] segments = proxyStr.split("\\|");
            String address = segments[0].replaceFirst("^https?://", "");
            String[] parts = address.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 7890;

            builder.proxy(ProxySelector.of(new InetSocketAddress(host, port)));
            System.out.println("[" + getProviderId() + "] 使用代理: " + host + ":" + port);

            if (segments.length >= 2 && !segments[1].isEmpty()) {
                String token = segments[1];
                try {
                    String decoded = new String(Base64.getDecoder().decode(token));
                    if (decoded.contains(":")) {
                        String[] creds = decoded.split(":", 2);
                        this.proxyAuthToken = "Basic " + Base64.getEncoder()
                            .encodeToString((creds[0] + ":" + creds[1]).getBytes());
                        System.out.println("[" + getProviderId() + "] 代理认证已配置");
                    } else {
                        this.proxyAuthToken = "Basic " + token;
                    }
                } catch (IllegalArgumentException e) {
                    this.proxyAuthToken = "Basic " + token;
                }
            }
        } catch (Exception e) {
            System.err.println("[" + getProviderId() + "] 代理配置解析失败: " + e.getMessage());
        }
    }

    @Override
    public List<GalleryImage> search(String keyword, int page, int pageSize) {
        int safePageSize = Math.min(pageSize, 30);
        String proxyAuthTokenSnapshot = this.proxyAuthToken;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                String url = buildSearchUrl(keyword, page, safePageSize);
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .GET();
                customizeRequest(reqBuilder);
                if (proxyAuthTokenSnapshot != null) {
                    reqBuilder.header("Proxy-Authorization", proxyAuthTokenSnapshot);
                }
                HttpRequest request = reqBuilder.build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();

                if (status == 200) {
                    return parseResults(response.body());
                } else if (status == 429) {
                    System.err.println("[" + getProviderId() + "] 请求被限流(429)，稍后重试");
                    if (attempt < MAX_RETRIES) {
                        Thread.sleep(1000 * (attempt + 1));
                        continue;
                    }
                } else if (status == 401 || status == 400) {
                    System.err.println("[" + getProviderId() + "] API密钥无效(" + status + ")");
                    return new ArrayList<>();
                } else if (status == 407) {
                    System.err.println("[" + getProviderId() + "] 代理认证失败(407)，请检查 HTTP_PROXY / HTTPS_PROXY");
                    return new ArrayList<>();
                } else if (status >= 500) {
                    System.err.println("[" + getProviderId() + "] 服务端错误(" + status + ")，重试中...");
                    if (attempt < MAX_RETRIES) {
                        sleepBeforeRetry(RETRY_DELAY_MS);
                        continue;
                    }
                } else {
                    System.err.println("[" + getProviderId() + "] API返回状态码: " + status);
                }
            } catch (java.net.ConnectException e) {
                System.err.println("[" + getProviderId() + "] 连接被拒绝: " + e.getMessage());
                break;
            } catch (java.net.http.HttpTimeoutException e) {
                System.err.println("[" + getProviderId() + "] 请求超时");
                if (attempt < MAX_RETRIES) {
                    sleepBeforeRetry(RETRY_DELAY_MS);
                    continue;
                }
                break;
            } catch (java.net.SocketTimeoutException e) {
                System.err.println("[" + getProviderId() + "] 连接超时");
                if (attempt < MAX_RETRIES) {
                    sleepBeforeRetry(RETRY_DELAY_MS);
                    continue;
                }
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (javax.net.ssl.SSLHandshakeException e) {
                System.err.println("[" + getProviderId() + "] TLS握手失败: " + e.getMessage());
                break;
            } catch (Exception e) {
                System.err.println("[" + getProviderId() + "] 搜索失败: " + e.getMessage());
                break;
            }
        }
        return new ArrayList<>();
    }

    /** 构建搜索URL，由子类实现 */
    protected abstract String buildSearchUrl(String keyword, int page, int pageSize);

    /** 自定义请求头，子类可覆盖 */
    protected void customizeRequest(HttpRequest.Builder builder) {}

    /** 解析搜索结果，由子类实现 */
    protected abstract List<GalleryImage> parseResults(String json);

    private void sleepBeforeRetry(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
