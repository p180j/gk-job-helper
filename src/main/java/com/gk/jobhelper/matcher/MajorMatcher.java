package com.gk.jobhelper.matcher;

import com.gk.jobhelper.common.MajorNameNormalizer;
import com.gk.jobhelper.common.TextNormalizer;
import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.MajorCatalog;
import com.gk.jobhelper.entity.MajorCatalogItem;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.service.MajorCatalogService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 专业资格匹配器（MAJOR）。
 *
 * 核心原则（禁止 AI 猜测专业关系）:
 * 1. 官方考试/招录单位专用目录优先，教育部通用目录其次
 * 2. 精确名称/代码匹配，禁止 contains / 相似度 / 字符串前缀推断
 * 3. 专业类归属关系全部通过数据库目录父子树判断
 * 4. 无法可靠判断时返回 UNCERTAIN，不为提高 MATCH 数量而猜测
 *
 * 判定规则:
 * - 不限专业表达                     -> MATCH
 * - 专业名称精确匹配（多候选 OR）      -> MATCH
 * - 专业代码精确匹配                  -> MATCH
 * - 用户专业属于岗位要求的专业类       -> MATCH（目录父子树验证）
 * - 名称与代码对应关系冲突             -> UNCERTAIN
 * - "相关专业/相近专业"等非标准表述    -> UNCERTAIN
 * - 括号排除（不含X）命中             -> NOT_MATCH
 * - 双方明确收录且无归属关系          -> NOT_MATCH
 */
@Component
public class MajorMatcher implements JobConditionMatcher {

    private final MajorCatalogService majorCatalogService;

    public MajorMatcher(MajorCatalogService majorCatalogService) {
        this.majorCatalogService = majorCatalogService;
    }

    @Override
    public ConditionType support() {
        return ConditionType.MAJOR;
    }

    @Override
    public MatchItemResult match(UserProfile profile, JobPosition position, MatchContext context) {
        String requirementRaw = position.getMajorRequirement();
        String majorRaw = profile.getMajor();
        String majorCodeRaw = profile.getMajorCode();
        String userValue = buildUserValue(majorRaw, majorCodeRaw);

        // 1. 岗位要求为空：业务约定为未设置专业限制
        if (TextNormalizer.isBlank(requirementRaw)) {
            return build(MatchResult.MATCH, userValue, requirementRaw, "岗位未设置专业要求，按不限专业处理。");
        }
        if (TextNormalizer.isBlank(majorRaw) && TextNormalizer.isBlank(majorCodeRaw)) {
            return build(MatchResult.UNCERTAIN, userValue, requirementRaw, "用户档案专业为空，无法判断专业要求。");
        }

        // 2. 解析岗位专业要求
        MajorRequirementParser.ParsedRequirement parsed =
                MajorRequirementParser.parse(requirementRaw, profile.getEducation());
        if (parsed.isParseFailure()) {
            return build(MatchResult.UNCERTAIN, userValue, requirementRaw, parsed.getFailureReason());
        }
        if (parsed.isUnlimited()) {
            return build(MatchResult.MATCH, userValue, requirementRaw, "岗位不限专业。");
        }

        // 3. 目录优先级链：考试绑定目录优先，其次教育部对应学历层级目录
        String educationLevel = MajorCatalogService.resolveEducationLevel(
                profile.getMajorEducationLevel(), profile.getEducation());
        List<MajorCatalogService.CatalogEntry> catalogs =
                majorCatalogService.resolveCatalogs(position.getExamId(), educationLevel);
        if (catalogs.isEmpty()) {
            return build(MatchResult.UNCERTAIN, userValue, requirementRaw,
                    "无可用专业目录（未绑定考试专用目录，且无法确定教育部对应学历层级目录），无法判断专业要求，需人工确认。");
        }

        // 4. 用户专业在各目录中的节点解析 + 名称/代码冲突检查
        UserResolution user = resolveUser(catalogs, majorRaw, majorCodeRaw);
        if (user.nameCodeConflict) {
            return build(MatchResult.UNCERTAIN, userValue, requirementRaw,
                    "用户专业名称“" + display(majorRaw) + "”与专业代码“" + display(majorCodeRaw)
                            + "”的对应关系在专业目录中存在冲突，需要人工确认。");
        }
        if (user.codeNotFound) {
            return build(MatchResult.UNCERTAIN, userValue, requirementRaw,
                    "用户专业代码“" + display(majorCodeRaw) + "”在可用专业目录中不存在，"
                            + "无法确认专业名称与代码的对应关系，需人工确认。");
        }
        if (!user.hasAnyNode()) {
            return build(MatchResult.UNCERTAIN, userValue, requirementRaw,
                    "用户专业“" + display(majorRaw) + "”未收录在可用专业目录中，无法确认其专业类别归属，需人工确认。");
        }

        // 5. 逐条件片段评估（片段间为 OR 关系）+ 聚合
        List<TokenVerdict> verdicts = new ArrayList<>();
        for (RequirementToken token : parsed.getTokens()) {
            verdicts.add(evaluateToken(token, catalogs, user, userValue));
        }
        return aggregate(verdicts, userValue, requirementRaw, userValue);
    }

    // =============================================================
    // 用户专业解析
    // =============================================================

    /** 用户专业在各目录中的节点解析结果（代码优先，代码未收录时按名称） */
    private UserResolution resolveUser(List<MajorCatalogService.CatalogEntry> catalogs,
                                       String majorRaw, String majorCodeRaw) {
        UserResolution resolution = new UserResolution();
        resolution.userComparisonName = MajorNameNormalizer.comparisonName(majorRaw);

        String code = MajorNameNormalizer.normalizeCode(majorCodeRaw);
        boolean codeProvided = !code.isEmpty();
        boolean codeResolved = false;
        boolean conflict = false;
        boolean anyNode = false;

        for (MajorCatalogService.CatalogEntry entry : catalogs) {
            MajorCatalog catalog = entry.getCatalog();
            List<MajorCatalogItem> byName = TextNormalizer.isBlank(majorRaw)
                    ? Collections.<MajorCatalogItem>emptyList()
                    : majorCatalogService.findByName(catalog, majorRaw);
            MajorCatalogItem byCode = codeProvided ? majorCatalogService.findByCode(catalog, code) : null;

            if (byCode != null) {
                codeResolved = true;
                // 名称与代码都解析到且完全不相交 -> 对应关系冲突，需人工确认，禁止自动选择
                if (!byName.isEmpty()) {
                    boolean overlap = false;
                    for (MajorCatalogItem item : byName) {
                        if (item.getId().equals(byCode.getId())) {
                            overlap = true;
                            break;
                        }
                    }
                    if (!overlap) {
                        conflict = true;
                    }
                }
                List<MajorCatalogItem> nodes = new ArrayList<>(1);
                nodes.add(byCode);
                resolution.nodesByCatalog.put(catalog.getId(), nodes);
                anyNode = true;
            } else {
                resolution.nodesByCatalog.put(catalog.getId(), byName);
                anyNode = anyNode || !byName.isEmpty();
            }
        }
        resolution.nameCodeConflict = conflict;
        resolution.codeNotFound = codeProvided && !codeResolved;
        resolution.anyNode = anyNode;
        return resolution;
    }

    /** 用户专业解析中间结果 */
    private static class UserResolution {
        /** catalogId -> 用户专业命中的目录节点（可能为空列表） */
        final Map<Long, List<MajorCatalogItem>> nodesByCatalog = new LinkedHashMap<>();
        /** 用户专业名称比较值（用于排除名单判断） */
        String userComparisonName = "";
        /** 某目录中名称与代码解析到不同节点（对应关系冲突） */
        boolean nameCodeConflict;
        /** 提供了代码但所有目录均未收录 */
        boolean codeNotFound;
        /** 至少一个目录解析到用户专业节点 */
        boolean anyNode;

        boolean hasAnyNode() {
            return anyNode;
        }

        List<MajorCatalogItem> nodesFor(Long catalogId) {
            List<MajorCatalogItem> nodes = nodesByCatalog.get(catalogId);
            return nodes == null ? Collections.<MajorCatalogItem>emptyList() : nodes;
        }
    }

    // =============================================================
    // 条件片段评估
    // =============================================================

    /** 单个条件片段的判定结果 */
    private static class TokenVerdict {
        final MatchResult result;
        final String reason;
        final MatchEvidence evidence;

        TokenVerdict(MatchResult result, String reason, MatchEvidence evidence) {
            this.result = result;
            this.reason = reason;
            this.evidence = evidence;
        }
    }

    private TokenVerdict evaluateToken(RequirementToken token, List<MajorCatalogService.CatalogEntry> catalogs,
                                       UserResolution user, String userValue) {
        // 无法可靠解析的括号限定内容：保守 UNCERTAIN
        if (token.isOpaque()) {
            return new TokenVerdict(MatchResult.UNCERTAIN,
                    "岗位专业要求“" + token.getRaw() + "”中包含无法可靠解析的括号限定内容，需人工确认。", null);
        }
        // 纯“相关专业”类非标准表述：UNCERTAIN
        if (token.isRelatedOnly()) {
            return new TokenVerdict(MatchResult.UNCERTAIN,
                    "岗位使用“" + token.getRaw() + "”等非标准范围表述，最终专业资格需以招录单位审核口径为准。", null);
        }

        // 按目录优先级层级评估：高优先级目录已明确判定的结果不被低优先级目录推翻；
        // 同优先级目录结论冲突 -> UNCERTAIN
        Map<Integer, List<TokenVerdict>> verdictsByTier = new LinkedHashMap<>();
        boolean requirementResolvedAnywhere = false;
        for (MajorCatalogService.CatalogEntry entry : catalogs) {
            boolean[] resolvedFlag = new boolean[1];
            TokenVerdict verdict = evaluateInCatalog(token, entry, user, userValue, resolvedFlag);
            requirementResolvedAnywhere = requirementResolvedAnywhere || resolvedFlag[0];
            if (verdict != null) {
                List<TokenVerdict> tierVerdicts =
                        verdictsByTier.computeIfAbsent(entry.getTier(), k -> new ArrayList<>());
                tierVerdicts.add(verdict);
            }
        }

        for (Map.Entry<Integer, List<TokenVerdict>> tier : verdictsByTier.entrySet()) {
            TokenVerdict match = null;
            TokenVerdict notMatch = null;
            for (TokenVerdict verdict : tier.getValue()) {
                if (verdict.result == MatchResult.MATCH) {
                    match = verdict;
                } else if (verdict.result == MatchResult.NOT_MATCH) {
                    notMatch = verdict;
                }
            }
            if (match != null && notMatch != null) {
                // 同优先级目录冲突：保留使用了哪些目录的信息
                return new TokenVerdict(MatchResult.UNCERTAIN,
                        "不同专业目录（" + catalogNamesOfTier(catalogs, tier.getKey())
                                + "）对该专业的分类存在冲突，需要按本次招录机关口径确认。", null);
            }
            if (match != null) {
                return match;
            }
            if (notMatch != null) {
                // “X及相关专业”：标准部分明确不满足时不直接 NOT_MATCH（相关专业部分需招录单位口径确认）
                if (token.isRelatedSuffix()) {
                    return new TokenVerdict(MatchResult.UNCERTAIN,
                            "用户专业不属于“" + token.getRaw() + "”中的标准专业范围，但岗位同时允许“相关专业”"
                                    + "等非标准表述，需以招录单位审核口径为准。", null);
                }
                return notMatch;
            }
        }

        // 所有目录均无法给出明确判定
        if (token.isRelatedSuffix()) {
            return new TokenVerdict(MatchResult.UNCERTAIN,
                    "岗位使用“" + token.getRaw() + "”等非标准范围表述，最终专业资格需以招录单位审核口径为准。", null);
        }
        if (!requirementResolvedAnywhere) {
            return new TokenVerdict(MatchResult.UNCERTAIN,
                    "岗位专业要求中的“" + token.getRaw() + "”未收录在可用专业目录中，无法可靠判断，需人工确认。", null);
        }
        return new TokenVerdict(MatchResult.UNCERTAIN,
                "用户专业与岗位要求“" + token.getRaw() + "”未收录于同一专业目录，无法确认归属关系，需人工确认。", null);
    }

    /** 同一优先级层级内的目录名称列表（冲突 reason 使用） */
    private String catalogNamesOfTier(List<MajorCatalogService.CatalogEntry> catalogs, int tier) {
        StringBuilder names = new StringBuilder();
        for (MajorCatalogService.CatalogEntry entry : catalogs) {
            if (entry.getTier() == tier) {
                if (names.length() > 0) {
                    names.append("、");
                }
                names.append(entry.getCatalog().getCatalogName());
            }
        }
        return names.toString();
    }

    /**
     * 在单个目录内评估条件片段。
     * 返回 null 表示该目录无法给出明确判定（要求或用户专业未收录 / 要求内部代码名称冲突）。
     */
    private TokenVerdict evaluateInCatalog(RequirementToken token, MajorCatalogService.CatalogEntry entry,
                                           UserResolution user, String userValue, boolean[] requirementResolved) {
        MajorCatalog catalog = entry.getCatalog();

        // 1. 解析岗位要求节点（代码 + 名称，取并集）
        MajorCatalogItem byCode = token.getCode() == null
                ? null : majorCatalogService.findByCode(catalog, token.getCode());
        List<MajorCatalogItem> byName = token.getName() == null
                ? Collections.<MajorCatalogItem>emptyList()
                : majorCatalogService.findByName(catalog, token.getName());
        if (byCode != null || !byName.isEmpty()) {
            requirementResolved[0] = true;
        }
        // 要求提供了代码但该目录未收录该代码：无法验证代码与名称的层级对应关系
        //（如研究生目录中的"0835 软件工程"不能按本科目录的"软件工程"名称判定），该目录不参与判定
        if (token.getCode() != null && byCode == null) {
            return null;
        }
        // 要求内部冲突：代码与名称都解析到但完全不相交 -> 该目录无法可靠评估
        if (byCode != null && !byName.isEmpty()) {
            boolean overlap = false;
            for (MajorCatalogItem item : byName) {
                if (item.getId().equals(byCode.getId())) {
                    overlap = true;
                    break;
                }
            }
            if (!overlap) {
                return null;
            }
        }
        Map<Long, MajorCatalogItem> reqDedupe = new LinkedHashMap<>();
        if (byCode != null) {
            reqDedupe.put(byCode.getId(), byCode);
        }
        for (MajorCatalogItem item : byName) {
            reqDedupe.put(item.getId(), item);
        }
        if (reqDedupe.isEmpty()) {
            return null;
        }

        // 2. 用户专业节点（该目录内）
        List<MajorCatalogItem> userNodes = user.nodesFor(catalog.getId());
        if (userNodes.isEmpty()) {
            return null;
        }

        // 3. 归属关系：相等或要求节点为用户节点祖先（全部基于目录 parent_id 树，禁止字符串前缀）
        MajorCatalogItem matchedUser = null;
        MajorCatalogItem matchedReq = null;
        for (MajorCatalogItem userNode : userNodes) {
            for (MajorCatalogItem reqNode : reqDedupe.values()) {
                if (majorCatalogService.isAncestorOrSelf(reqNode, userNode)) {
                    matchedUser = userNode;
                    matchedReq = reqNode;
                    break;
                }
            }
            if (matchedUser != null) {
                break;
            }
        }

        if (matchedUser != null) {
            // 括号排除（不含X）：命中排除名单 -> NOT_MATCH
            if (isExcluded(token, matchedUser, user.userComparisonName)) {
                return new TokenVerdict(MatchResult.NOT_MATCH,
                        "岗位专业要求“" + token.getRaw() + "”明确排除用户专业“"
                                + nodeDisplay(matchedUser) + "”。", null);
            }
            String reason;
            if (matchedReq.getId().equals(matchedUser.getId())) {
                reason = "用户专业“" + nodeDisplay(matchedUser) + "”与岗位专业要求“"
                        + nodeDisplay(matchedReq) + "”一致，满足专业要求。";
            } else {
                reason = "用户专业“" + nodeDisplay(matchedUser) + "”属于官方专业目录“"
                        + catalog.getCatalogName() + "”中的“" + nodeDisplay(matchedReq)
                        + "”，满足岗位专业要求。";
            }
            MatchEvidence evidence = new MatchEvidence(
                    catalog.getCatalogCode(), catalog.getCatalogName(),
                    matchedUser.getMajorCode(), matchedUser.getMajorName(),
                    matchedReq.getMajorCode(), matchedReq.getMajorName());
            return new TokenVerdict(MatchResult.MATCH, reason, evidence);
        }

        // 双方均明确收录且无归属关系 -> NOT_MATCH
        return new TokenVerdict(MatchResult.NOT_MATCH,
                "用户专业“" + userValue + "”与岗位要求“" + token.getRaw() + "”在专业目录“"
                        + catalog.getCatalogName() + "”中无归属关系。", null);
    }

    /** 排除名单比较：用户名称比较值或命中节点名称比较值与排除项精确相等 */
    private boolean isExcluded(RequirementToken token, MajorCatalogItem matchedUser, String userComparisonName) {
        if (token.getExcludedNames().isEmpty()) {
            return false;
        }
        List<String> excludedComparison = new ArrayList<>();
        for (String excluded : token.getExcludedNames()) {
            String comparison = MajorNameNormalizer.comparisonName(excluded);
            if (!comparison.isEmpty() && !excludedComparison.contains(comparison)) {
                excludedComparison.add(comparison);
            }
        }
        if (!userComparisonName.isEmpty() && excludedComparison.contains(userComparisonName)) {
            return true;
        }
        String nodeComparison = MajorNameNormalizer.comparisonName(matchedUser.getMajorName());
        return !nodeComparison.isEmpty() && excludedComparison.contains(nodeComparison);
    }

    // =============================================================
    // 聚合
    // =============================================================

    /** 片段间 OR 语义：任一 MATCH -> MATCH；否则任一 UNCERTAIN -> UNCERTAIN；否则 NOT_MATCH */
    private MatchItemResult aggregate(List<TokenVerdict> verdicts, String userValue,
                                      String requirementValue, String userDisplay) {
        TokenVerdict match = null;
        TokenVerdict uncertain = null;
        TokenVerdict notMatch = null;
        for (TokenVerdict verdict : verdicts) {
            if (verdict.result == MatchResult.MATCH && match == null) {
                match = verdict;
            } else if (verdict.result == MatchResult.UNCERTAIN && uncertain == null) {
                uncertain = verdict;
            } else if (verdict.result == MatchResult.NOT_MATCH && notMatch == null) {
                notMatch = verdict;
            }
        }
        if (match != null) {
            return build(MatchResult.MATCH, userValue, requirementValue, match.reason, match.evidence);
        }
        if (uncertain != null) {
            return build(MatchResult.UNCERTAIN, userValue, requirementValue, uncertain.reason);
        }
        if (notMatch != null) {
            if (verdicts.size() > 1) {
                return build(MatchResult.NOT_MATCH, userValue, requirementValue,
                        "用户专业“" + userDisplay + "”不属于岗位要求的任一专业/类别。");
            }
            return build(MatchResult.NOT_MATCH, userValue, requirementValue, notMatch.reason);
        }
        // 无片段（理论不可达：解析成功至少一个 token）
        return build(MatchResult.UNCERTAIN, userValue, requirementValue, "无法判断专业要求，需人工确认。");
    }

    // =============================================================
    // 工具
    // =============================================================

    private MatchItemResult build(MatchResult result, String userValue, String requirementValue, String reason) {
        return new MatchItemResult(ConditionType.MAJOR, result, userValue, requirementValue, reason);
    }

    private MatchItemResult build(MatchResult result, String userValue, String requirementValue,
                                  String reason, MatchEvidence evidence) {
        return new MatchItemResult(ConditionType.MAJOR, result, userValue, requirementValue, reason, evidence);
    }

    /** 用户专业展示值：名称(代码) */
    private String buildUserValue(String majorRaw, String majorCodeRaw) {
        boolean hasMajor = !TextNormalizer.isBlank(majorRaw);
        boolean hasCode = !TextNormalizer.isBlank(majorCodeRaw);
        if (hasMajor && hasCode) {
            return majorRaw.trim() + "(" + majorCodeRaw.trim() + ")";
        }
        if (hasMajor) {
            return majorRaw.trim();
        }
        if (hasCode) {
            return majorCodeRaw.trim();
        }
        return "";
    }

    /** 目录节点展示值：名称(代码) */
    private String nodeDisplay(MajorCatalogItem node) {
        if (node.getMajorCode() == null || node.getMajorCode().isEmpty()) {
            return node.getMajorName();
        }
        return node.getMajorName() + "(" + node.getMajorCode() + ")";
    }

    private String display(String raw) {
        return raw == null || raw.trim().isEmpty() ? "空" : raw.trim();
    }
}
