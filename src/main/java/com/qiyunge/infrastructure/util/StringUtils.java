package com.qiyunge.infrastructure.util;

/**
 * 字符串工具类：提供通用的字符串操作方法。
 */
public final class StringUtils {

    private StringUtils() {}

    /**
     * 返回第一个非空白的字符串，全部为空则返回 null。
     */
    public static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
