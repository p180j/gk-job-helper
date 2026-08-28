package com.gk.jobhelper.common;

/**
 * 专业名称/代码标准化工具。
 * - 名称标准化: 全角/半角统一、中文空格/换行清理、括号统一、连续空格压缩
 * - 比较值: 在标准化基础上额外去除末尾"专业"二字（仅用于比较，原始值保留用于 reason）
 * - 代码标准化: 去空格、全角转半角、字母转大写
 *
 * 禁止模糊分词后自动认为相似专业相同。
 */
public final class MajorNameNormalizer {

    private static final String MAJOR_SUFFIX = "专业";

    private MajorNameNormalizer() {
    }

    /**
     * 名称标准化：TextNormalizer 基础上统一中英文括号为中文括号（），
     * 并去除名称内部多余空白（如 "数据 科学" -> "数据科学"）。
     */
    public static String normalizeName(String name) {
        String normalized = TextNormalizer.normalize(name);
        if (normalized.isEmpty()) {
            return "";
        }
        // 括号统一为中文括号（中文目录的标准写法）
        normalized = normalized.replace("(", "（").replace(")", "）");
        // 专业名称内部不应有空格（"软件 工程" 多为排版噪声），比较时去除全部空格
        normalized = normalized.replace(" ", "");
        return normalized;
    }

    /**
     * 比较值：标准化 + 去除末尾"专业"二字。
     * 例如 "软件工程专业" -> "软件工程"（仅用于比较，原始值仍保留在 reason 中）。
     */
    public static String comparisonName(String name) {
        String normalized = normalizeName(name);
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.endsWith(MAJOR_SUFFIX) && normalized.length() > MAJOR_SUFFIX.length()) {
            return normalized.substring(0, normalized.length() - MAJOR_SUFFIX.length());
        }
        return normalized;
    }

    /**
     * 代码标准化：去空格、全角转半角、字母统一大写。
     * 例如 "0809 04k" -> "080904K"，"０８０９０２" -> "080902"。
     */
    public static String normalizeCode(String code) {
        String normalized = TextNormalizer.normalize(code);
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized.replace(" ", "").toUpperCase();
    }
}
