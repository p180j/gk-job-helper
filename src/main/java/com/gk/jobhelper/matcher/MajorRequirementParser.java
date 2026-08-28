package com.gk.jobhelper.matcher;

import com.gk.jobhelper.common.MajorNameNormalizer;
import com.gk.jobhelper.common.TextNormalizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 岗位专业要求解析器（纯函数，无状态）。
 *
 * 支持的表达（保守解析，无法稳定解析时返回 parseFailure，由 Matcher 判 UNCERTAIN，禁止猜测）:
 * - 不限专业类表达: 不限 / 专业不限 / 不限专业 / 无专业限制 / 无要求
 * - 分学历层次表达: "本科:计算机类;研究生:法学"（按用户学历选择对应部分）
 * - 多专业/类并列: 以 , ， 、 / ; ； 分隔
 * - 代码 + 名称:  "080902 软件工程" / "0809计算机类"
 * - 括号排除:     "计算机类(不含数字媒体技术)"
 * - 相关专业:     "计算机相关专业" / "计算机类及相关专业"
 */
public final class MajorRequirementParser {

    /** 片段分隔符：半角逗号、中文顿号、斜杠、分号（全角已被 TextNormalizer 转半角） */
    private static final Pattern SEPARATOR = Pattern.compile("[,、/;]+");

    /** 括号排除: (不含X) / (不包括X)，可多组 */
    private static final Pattern EXCLUSION = Pattern.compile("\\((不含|不包括)([^)]*)\\)");

    /** 其余括号内容（可能是代码注释或方向注记） */
    private static final Pattern PARENTHESIS = Pattern.compile("\\(([^)]*)\\)");

    /** 前导专业/类代码: 2 位以上数字开头，可带 K/T 等字母 */
    private static final Pattern LEADING_CODE = Pattern.compile("^([0-9][0-9A-Z]+)\\s*(.*)$");

    /** 相关专业类后缀（长表达式优先） */
    private static final String[] RELATED_SUFFIXES = {
            "及其他相关专业", "及相关专业", "其他相关专业", "相关专业", "相近专业", "相似专业", "相关学科"
    };

    /** 分学历层次前缀 */
    private static final Pattern LEVEL_PREFIX = Pattern.compile("^([^:]{1,8}):(.+)$");

    private static final String[] LEVEL_KEYWORDS = {
            "硕士研究生", "博士研究生", "研究生", "本科", "大专", "专科", "硕士", "博士"
    };

    private MajorRequirementParser() {
    }

    /**
     * 解析岗位专业要求。
     *
     * @param requirement    岗位专业要求原始文本
     * @param userEducation  用户学历（用于"本科:xx;研究生:xx"分段表达时选择对应部分），可空
     */
    public static ParsedRequirement parse(String requirement, String userEducation) {
        String text = TextNormalizer.normalize(requirement);
        if (text.isEmpty()) {
            return ParsedRequirement.failure("岗位专业要求为空，无法可靠判断。");
        }
        if (isUnlimitedMajor(text)) {
            return ParsedRequirement.unlimited();
        }

        // 分学历层次表达处理
        String effectiveText = text;
        String leveled = selectLeveledSegment(text, userEducation);
        if (leveled == null) {
            return ParsedRequirement.failure("岗位专业要求按学历层次分段表述，无法根据用户学历确定适用的专业要求，需人工确认。");
        }
        if (!leveled.isEmpty()) {
            effectiveText = leveled;
        }

        List<RequirementToken> tokens = new ArrayList<>();
        for (String segment : SEPARATOR.split(effectiveText)) {
            String token = segment.trim();
            if (token.isEmpty()) {
                continue;
            }
            RequirementToken parsed = parseToken(token);
            if (parsed != null) {
                tokens.add(parsed);
            }
        }
        if (tokens.isEmpty()) {
            return ParsedRequirement.failure("无法从岗位专业要求中解析出有效的专业条件，需人工确认。");
        }
        return ParsedRequirement.of(tokens);
    }

    /** 是否"不限专业"类表达 */
    static boolean isUnlimitedMajor(String normalizedText) {
        if (TextNormalizer.isUnlimited(normalizedText)) {
            return true;
        }
        String t = TextNormalizer.normalize(normalizedText);
        return "专业不限".equals(t) || "不限专业".equals(t) || "无专业限制".equals(t);
    }

    /**
     * 分学历层次表达处理。
     * 返回值含义: null=解析失败(格式不稳或无法选择层次); ""=非分段表达(使用原文); 其他=选中的分段内容。
     */
    private static String selectLeveledSegment(String text, String userEducation) {
        String[] segments = text.split(";", -1);
        boolean anyLeveled = false;
        Map<String, String> leveledSegments = new LinkedHashMap<>();
        for (String segment : segments) {
            String s = segment.trim();
            if (s.isEmpty()) {
                continue;
            }
            Matcher matcher = LEVEL_PREFIX.matcher(s);
            if (matcher.matches() && isLevelKeyword(matcher.group(1).trim())) {
                anyLeveled = true;
                leveledSegments.put(matcher.group(1).trim(), matcher.group(2).trim());
            } else if (anyLeveled) {
                // 部分分段、部分普通文本 -> 无法稳定解析
                return null;
            }
        }
        if (!anyLeveled) {
            return "";
        }
        String userLevel = userLevelKeyword(TextNormalizer.normalize(userEducation));
        if (userLevel == null) {
            return null;
        }
        for (Map.Entry<String, String> entry : leveledSegments.entrySet()) {
            if (matchesUserLevel(userLevel, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static boolean isLevelKeyword(String keyword) {
        for (String level : LEVEL_KEYWORDS) {
            if (level.equals(keyword)) {
                return true;
            }
        }
        return false;
    }

    /** 用户学历文本 -> 标准层次关键词（本科/专科/研究生） */
    static String userLevelKeyword(String normalizedEducation) {
        if (normalizedEducation == null || normalizedEducation.isEmpty()) {
            return null;
        }
        if (normalizedEducation.contains("博士") || normalizedEducation.contains("硕士")
                || normalizedEducation.contains("研究生")) {
            return "研究生";
        }
        if (normalizedEducation.contains("本科")) {
            return "本科";
        }
        if (normalizedEducation.contains("专科") || normalizedEducation.contains("大专")
                || normalizedEducation.contains("高职")) {
            return "专科";
        }
        return null;
    }

    /** 分段前缀是否对应用户学历层次 */
    private static boolean matchesUserLevel(String userLevel, String segmentPrefix) {
        if ("本科".equals(userLevel)) {
            return "本科".equals(segmentPrefix);
        }
        if ("专科".equals(userLevel)) {
            return "专科".equals(segmentPrefix) || "大专".equals(segmentPrefix);
        }
        // 研究生
        return "研究生".equals(segmentPrefix) || "硕士".equals(segmentPrefix) || "博士".equals(segmentPrefix)
                || "硕士研究生".equals(segmentPrefix) || "博士研究生".equals(segmentPrefix);
    }

    /** 解析单个片段 */
    private static RequirementToken parseToken(String token) {
        String base = token;
        List<String> excludedNames = new ArrayList<>();

        // 1. 提取括号排除项: (不含X) / (不包括X)
        Matcher exclusionMatcher = EXCLUSION.matcher(base);
        StringBuilder withoutExclusions = new StringBuilder();
        int last = 0;
        while (exclusionMatcher.find()) {
            withoutExclusions.append(base, last, exclusionMatcher.start());
            for (String name : exclusionMatcher.group(2).split("[,、/]+")) {
                String trimmed = name.trim();
                if (!trimmed.isEmpty()) {
                    excludedNames.add(trimmed);
                }
            }
            last = exclusionMatcher.end();
        }
        withoutExclusions.append(base.substring(last));
        base = withoutExclusions.toString().trim();

        // 2. 其余括号内容: 代码注释(如"计算机类(0809)")并入代码提取；
        //    其他无法可靠解析的限定内容标记 opaque，由 Matcher 保守判 UNCERTAIN（禁止静默丢弃）
        String codeFromParenthesis = null;
        boolean opaque = false;
        Matcher parenthesisMatcher = PARENTHESIS.matcher(base);
        StringBuilder withoutParenthesis = new StringBuilder();
        last = 0;
        while (parenthesisMatcher.find()) {
            withoutParenthesis.append(base, last, parenthesisMatcher.start());
            String inner = parenthesisMatcher.group(1).trim();
            if (MajorNameNormalizer.normalizeCode(inner).matches("[0-9][0-9A-Z]+")) {
                codeFromParenthesis = MajorNameNormalizer.normalizeCode(inner);
            } else if (!inner.isEmpty()) {
                opaque = true;
            }
            last = parenthesisMatcher.end();
        }
        withoutParenthesis.append(base.substring(last));
        base = withoutParenthesis.toString().trim();

        // 3. "相关专业"类表达剥离
        boolean relatedOnly = false;
        boolean relatedSuffix = false;
        String stripped = base;
        for (String suffix : RELATED_SUFFIXES) {
            if (stripped.endsWith(suffix)) {
                stripped = stripped.substring(0, stripped.length() - suffix.length()).trim();
                relatedSuffix = true;
                break;
            }
        }
        if (stripped.isEmpty() && (relatedSuffix || base.contains("相关") || base.contains("相近"))) {
            relatedOnly = true;
            return new RequirementToken(token, null, null, relatedOnly, false, excludedNames, opaque);
        }
        base = stripped;

        // 4. 前导代码提取
        String code = codeFromParenthesis;
        String name = null;
        Matcher codeMatcher = LEADING_CODE.matcher(base);
        if (codeMatcher.matches()) {
            if (code == null) {
                code = codeMatcher.group(1);
            }
            String rest = codeMatcher.group(2).trim();
            if (!rest.isEmpty()) {
                name = rest;
            }
        } else if (!base.isEmpty()) {
            name = base;
        }

        if (code == null && name == null) {
            // 无法解析出任何有效内容（如纯排除表达"不含X"单独成段）
            return new RequirementToken(token, null, null, true, false, excludedNames, opaque);
        }
        return new RequirementToken(token, code, name, relatedOnly, relatedSuffix, excludedNames, opaque);
    }

    /**
     * 解析结果：unlimited / parseFailure / tokens 三态
     */
    public static class ParsedRequirement {

        private final boolean unlimited;
        private final boolean parseFailure;
        private final String failureReason;
        private final List<RequirementToken> tokens;

        private ParsedRequirement(boolean unlimited, boolean parseFailure, String failureReason,
                                  List<RequirementToken> tokens) {
            this.unlimited = unlimited;
            this.parseFailure = parseFailure;
            this.failureReason = failureReason;
            this.tokens = tokens == null ? new ArrayList<>() : tokens;
        }

        static ParsedRequirement unlimited() {
            return new ParsedRequirement(true, false, null, null);
        }

        static ParsedRequirement failure(String reason) {
            return new ParsedRequirement(false, true, reason, null);
        }

        static ParsedRequirement of(List<RequirementToken> tokens) {
            return new ParsedRequirement(false, false, null, tokens);
        }

        public boolean isUnlimited() {
            return unlimited;
        }

        public boolean isParseFailure() {
            return parseFailure;
        }

        public String getFailureReason() {
            return failureReason;
        }

        public List<RequirementToken> getTokens() {
            return tokens;
        }
    }
}
