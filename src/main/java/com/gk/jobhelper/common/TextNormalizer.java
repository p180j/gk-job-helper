package com.gk.jobhelper.common;

/**
 * 匹配输入字符串标准化工具：
 * - 去除首尾空白
 * - 全角转半角（含全角空格 \u3000）
 * - 去除换行/制表符等控制字符
 * - 连续空白压缩为单个空格
 * 供各 Matcher 在解析前统一调用，避免全角符号/中文空格/换行导致规则漏匹配。
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /**
     * 标准化文本：全角转半角、去控制字符、压缩空白。
     * 仅用于规则解析，reason 中展示的值仍使用原始输入。
     */
    public static String normalize(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u3000') {            // 全角空格 -> 半角空格
                sb.append(' ');
            } else if (c >= '\uFF01' && c <= '\uFF5E') { // 全角字符 -> 半角
                sb.append((char) (c - 0xFEE0));
            } else if (c == '\n' || c == '\r' || c == '\t') { // 换行/制表 -> 空格
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        // 压缩连续空格并去首尾
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    /** 标准化后是否为空（null / 空白 / 仅空白字符） */
    public static boolean isBlank(String text) {
        return normalize(text).isEmpty();
    }

    /**
     * 是否表达"不限制/无要求"：
     * 不限 / 无限制 / 无要求 / 无 / 不作要求 / 没有限制
     */
    public static boolean isUnlimited(String normalizedText) {
        String t = normalize(normalizedText);
        return "不限".equals(t) || "无限制".equals(t) || "无要求".equals(t)
                || "无".equals(t) || "不作要求".equals(t) || "没有限制".equals(t);
    }
}
