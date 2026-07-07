package com.qiyunge.infrastructure.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具类：统一的日期解析格式。
 */
public final class DateTimeUtil {

    private DateTimeUtil() {}

    /** 标准日期时间格式（yyyy-MM-dd HH:mm:ss） */
    public static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 解析日期时间字符串，null 或空值返回 null。
     */
    public static LocalDateTime parseDateTime(String value) {
        if (value == null || value.isEmpty()) return null;
        try { return LocalDateTime.parse(value, DT_FMT); }
        catch (Exception e) { return null; }
    }
}
