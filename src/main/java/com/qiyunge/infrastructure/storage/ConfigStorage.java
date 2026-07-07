package com.qiyunge.infrastructure.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ConfigStorage {

    private final Path configPath;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, String> cache;
    private final ScheduledExecutorService flushExecutor =
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "config-flush");
            t.setDaemon(true);
            return t;
        });
    private ScheduledFuture<?> pendingFlush;

    public ConfigStorage(AppStorage appStorage) {
        this.configPath = appStorage.getConfigPath();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.cache = new ConcurrentHashMap<>();
        loadFromFile();
    }

    private void loadFromFile() {
        File file = configPath.toFile();
        if (file.exists()) {
            try {
                ObjectNode node = (ObjectNode) objectMapper.readTree(file);
                node.fields().forEachRemaining(entry -> cache.put(entry.getKey(), entry.getValue().asText()));
            } catch (Exception e) {
                // Ignore parse errors, use defaults
            }
        }
    }

    private synchronized void saveToFile() {
        try {
            ObjectNode node = objectMapper.createObjectNode();
            cache.forEach(node::put);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), node);
        } catch (Exception e) {
            // Ignore write errors
        }
    }

    public String get(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    public void set(String key, String value) {
        cache.put(key, value);
        scheduleFlush();
    }

    /** 安排延迟写入（500ms 内的多次 set 只触发一次写入） */
    private void scheduleFlush() {
        if (pendingFlush != null) pendingFlush.cancel(false);
        pendingFlush = flushExecutor.schedule(this::saveToFile, 500, TimeUnit.MILLISECONDS);
    }

    /** 立即写入磁盘（应用关闭时调用） */
    public void flush() {
        if (pendingFlush != null) {
            pendingFlush.cancel(false);
            pendingFlush = null;
        }
        saveToFile();
    }

    /** 关闭资源 */
    public void shutdown() {
        flush();
        flushExecutor.shutdown();
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)));
    }
}
