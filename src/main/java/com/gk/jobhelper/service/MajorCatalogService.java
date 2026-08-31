package com.gk.jobhelper.service;

import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.common.MajorNameNormalizer;
import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.constant.CatalogType;
import com.gk.jobhelper.constant.MajorEducationLevel;
import com.gk.jobhelper.dto.MajorCatalogItemVO;
import com.gk.jobhelper.dto.MajorSearchItemVO;
import com.gk.jobhelper.dto.PageVO;
import com.gk.jobhelper.entity.ExamMajorCatalog;
import com.gk.jobhelper.entity.MajorCatalog;
import com.gk.jobhelper.entity.MajorCatalogItem;
import com.gk.jobhelper.mapper.ExamMajorCatalogMapper;
import com.gk.jobhelper.mapper.MajorCatalogItemMapper;
import com.gk.jobhelper.mapper.MajorCatalogMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 专业目录服务：
 * - 目录优先级链解析（考试绑定目录优先，其次教育部对应学历层级目录）
 * - 节点按代码 / 标准化名称 / 别名查询（精确匹配，禁止模糊相似推断）
 * - 父子树关系查询（祖先 / 后代，全部基于 parent_id，禁止字符串前缀判断）
 * - 目录管理基础查询（供 Controller 使用）
 */
@Service
public class MajorCatalogService {

    /** 教育部目录兜底层级基数：保证排在考试绑定目录（priority 通常为个位/十位数）之后 */
    private static final int MOE_TIER_BASE = 1_000_000;

    private static final int MAX_SEARCH_LIMIT = 100;

    private final MajorCatalogMapper majorCatalogMapper;
    private final MajorCatalogItemMapper majorCatalogItemMapper;
    private final ExamMajorCatalogMapper examMajorCatalogMapper;

    public MajorCatalogService(MajorCatalogMapper majorCatalogMapper,
                               MajorCatalogItemMapper majorCatalogItemMapper,
                               ExamMajorCatalogMapper examMajorCatalogMapper) {
        this.majorCatalogMapper = majorCatalogMapper;
        this.majorCatalogItemMapper = majorCatalogItemMapper;
        this.examMajorCatalogMapper = examMajorCatalogMapper;
    }

    // =============================================================
    // 目录优先级链解析
    // =============================================================

    /**
     * 解析可用的专业目录链（按有效优先级 tier 升序）：
     * 1. exam_major_catalog 中绑定的目录（按绑定 priority 升序，tier = 绑定 priority）
     * 2. 教育部(MOE)对应学历层级目录（tier = MOE_TIER_BASE + 目录 priority，恒在考试绑定目录之后）
     * 同一目录重复绑定时只保留最高优先级一处；已绑定的目录不再进入兜底层级。
     *
     * @param examId          考试 id（可空：无考试上下文时仅使用教育部目录）
     * @param educationLevel  学历层级（UNDERGRADUATE/GRADUATE/VOCATIONAL，可空：无法确定层级时仅使用考试绑定目录）
     */
    public List<CatalogEntry> resolveCatalogs(Long examId, String educationLevel) {
        List<CatalogEntry> entries = new ArrayList<>();
        Set<Long> addedCatalogIds = new LinkedHashSet<>();

        if (examId != null) {
            List<ExamMajorCatalog> bindings = examMajorCatalogMapper.selectByExamId(examId);
            for (ExamMajorCatalog binding : bindings) {
                MajorCatalog catalog = majorCatalogMapper.selectById(binding.getCatalogId());
                if (catalog == null || !Boolean.TRUE.equals(catalog.getEnabled())) {
                    continue;
                }
                if (educationLevel != null && !educationLevel.isEmpty()
                        && !educationLevel.equals(catalog.getEducationLevel())
                        && !"MIXED".equals(catalog.getEducationLevel())) {
                    continue;
                }
                if (addedCatalogIds.add(catalog.getId())) {
                    entries.add(new CatalogEntry(catalog, binding.getPriority() == null
                            ? 100 : binding.getPriority()));
                }
            }
        }

        if (educationLevel != null && !educationLevel.isEmpty()) {
            List<MajorCatalog> moeCatalogs = majorCatalogMapper.selectEnabledByTypeAndLevel(
                    CatalogType.MOE.name(), educationLevel);
            for (MajorCatalog catalog : moeCatalogs) {
                if (addedCatalogIds.add(catalog.getId())) {
                    entries.add(new CatalogEntry(catalog, MOE_TIER_BASE
                            + (catalog.getPriority() == null ? 100 : catalog.getPriority())));
                    // 教育部兜底目录只使用同层级优先级最高的一个版本；
                    // 旧版不能与本次考试已选版本并列参与判定而造成目录冲突。
                    break;
                }
            }
        }

        entries.sort((a, b) -> {
            int byTier = Integer.compare(a.getTier(), b.getTier());
            return byTier != 0 ? byTier : Long.compare(a.getCatalog().getId(), b.getCatalog().getId());
        });
        return entries;
    }

    /** 目录链条目：目录 + 有效优先级（同 tier 视为同优先级，结果冲突时需人工确认） */
    public static class CatalogEntry {

        private final MajorCatalog catalog;
        private final int tier;

        CatalogEntry(MajorCatalog catalog, int tier) {
            this.catalog = catalog;
            this.tier = tier;
        }

        public MajorCatalog getCatalog() {
            return catalog;
        }

        public int getTier() {
            return tier;
        }
    }

    // =============================================================
    // 节点查询（全部精确匹配）
    // =============================================================

    /** 目录内按标准化代码精确查询（代码在目录内唯一） */
    public MajorCatalogItem findByCode(MajorCatalog catalog, String rawCode) {
        if (catalog == null || rawCode == null) {
            return null;
        }
        String code = MajorNameNormalizer.normalizeCode(rawCode);
        if (code.isEmpty()) {
            return null;
        }
        return majorCatalogItemMapper.selectByCatalogAndCode(catalog.getId(), code);
    }

    /**
     * 目录内按名称查询：标准化名称 / 比较值（去"专业"后缀）精确匹配节点名称，
     * 并包含官方与人工维护别名的精确匹配。禁止模糊 contains。
     */
    public List<MajorCatalogItem> findByName(MajorCatalog catalog, String rawName) {
        if (catalog == null || rawName == null) {
            return new ArrayList<>();
        }
        List<String> names = comparisonNames(rawName);
        if (names.isEmpty()) {
            return new ArrayList<>();
        }
        List<MajorCatalogItem> result = new ArrayList<>();
        Map<Long, MajorCatalogItem> dedupe = new LinkedHashMap<>();
        for (MajorCatalogItem item : majorCatalogItemMapper.selectByCatalogAndNames(catalog.getId(), names)) {
            dedupe.put(item.getId(), item);
        }
        for (MajorCatalogItem item : majorCatalogItemMapper.selectByCatalogAndAliasNames(catalog.getId(), names)) {
            dedupe.put(item.getId(), item);
        }
        result.addAll(dedupe.values());
        return result;
    }

    /** 名称的候选比较值：比较值(去"专业"后缀) + 标准化名称，去重去空 */
    public static List<String> comparisonNames(String rawName) {
        List<String> names = new ArrayList<>();
        String comparison = MajorNameNormalizer.comparisonName(rawName);
        String normalized = MajorNameNormalizer.normalizeName(rawName);
        if (!comparison.isEmpty() && !names.contains(comparison)) {
            names.add(comparison);
        }
        if (!normalized.isEmpty() && !names.contains(normalized)) {
            names.add(normalized);
        }
        return names;
    }

    // =============================================================
    // 父子树关系（全部基于 parent_id 链，禁止字符串前缀判断）
    // =============================================================

    /**
     * 节点的全部祖先（从直接父节点到根，按自下而上顺序）。
     * 防御目录脏数据造成的环：超过 16 层即中止。
     */
    public List<MajorCatalogItem> getAncestors(MajorCatalogItem node) {
        List<MajorCatalogItem> ancestors = new ArrayList<>();
        if (node == null || node.getParentId() == null) {
            return ancestors;
        }
        Long currentId = node.getParentId();
        int depth = 0;
        Set<Long> visited = new LinkedHashSet<>();
        while (currentId != null && depth < 16 && visited.add(currentId)) {
            MajorCatalogItem parent = majorCatalogItemMapper.selectById(currentId);
            if (parent == null) {
                break;
            }
            ancestors.add(parent);
            currentId = parent.getParentId();
            depth++;
        }
        return ancestors;
    }

    /** ancestor 是否为 node 的祖先（含相等） */
    public boolean isAncestorOrSelf(MajorCatalogItem ancestor, MajorCatalogItem node) {
        if (ancestor == null || node == null) {
            return false;
        }
        if (ancestor.getId().equals(node.getId())) {
            return true;
        }
        for (MajorCatalogItem item : getAncestors(node)) {
            if (ancestor.getId().equals(item.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回用于历史分同类比较的稳定专业锚点。锚点只来自官方目录的 parent_id 树：
     * 本科/职教的具体专业向上取 CLASS；研究生的 DISCIPLINE/FIELD 取自身；
     * CATEGORY（如工学）绝不作为锚点。
     */
    public String similarityAnchorKey(Long itemId) {
        if (itemId == null) {
            return null;
        }
        MajorCatalogItem item = majorCatalogItemMapper.selectById(itemId);
        if (item == null) {
            return null;
        }
        MajorCatalogItem anchor = similarityAnchor(item);
        if (anchor == null || anchor.getMajorCode() == null) {
            return null;
        }
        MajorCatalog catalog = majorCatalogMapper.selectById(anchor.getCatalogId());
        if (catalog == null || catalog.getEducationLevel() == null) {
            return null;
        }
        return catalog.getEducationLevel() + "|" + anchor.getItemLevel() + "|"
                + MajorNameNormalizer.normalizeCode(anchor.getMajorCode());
    }

    private MajorCatalogItem similarityAnchor(MajorCatalogItem item) {
        if ("CATEGORY".equals(item.getItemLevel())) {
            return null;
        }
        if ("CLASS".equals(item.getItemLevel()) || "DISCIPLINE".equals(item.getItemLevel())
                || "FIELD".equals(item.getItemLevel())) {
            return item;
        }
        if ("MAJOR".equals(item.getItemLevel())) {
            for (MajorCatalogItem ancestor : getAncestors(item)) {
                if ("CLASS".equals(ancestor.getItemLevel())) {
                    return ancestor;
                }
            }
        }
        // 没有专业类父节点时保留具体官方节点，绝不退回到学科门类。
        return item;
    }

    /** 节点的全部后代（深度优先，按 sort_no 排序；防御环） */
    public List<MajorCatalogItem> getDescendants(MajorCatalogItem node) {
        List<MajorCatalogItem> descendants = new ArrayList<>();
        if (node == null) {
            return descendants;
        }
        collectChildren(node, descendants, new LinkedHashSet<Long>());
        return descendants;
    }

    private void collectChildren(MajorCatalogItem node, List<MajorCatalogItem> descendants, Set<Long> visited) {
        if (!visited.add(node.getId())) {
            return;
        }
        List<MajorCatalogItem> children = majorCatalogItemMapper.selectChildren(node.getCatalogId(), node.getId());
        for (MajorCatalogItem child : children) {
            descendants.add(child);
            collectChildren(child, descendants, visited);
        }
    }

    // =============================================================
    // 学历层级推断
    // =============================================================

    /**
     * 用户专业对应学历层级（枚举值）：
     * major_education_level 优先（本科/研究生/专科），为空时按最高学历 education 推断；
     * 均无法识别时返回 null（此时仅使用考试绑定目录，找不到则无法判断）。
     */
    public static String resolveEducationLevel(String majorEducationLevel, String education) {
        String level = levelKeyword(majorEducationLevel);
        if (level != null) {
            return level;
        }
        return levelKeyword(education);
    }

    /** 学历文本 -> MajorEducationLevel 枚举值 */
    private static String levelKeyword(String text) {
        String normalized = com.gk.jobhelper.common.TextNormalizer.normalize(text);
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.contains("博士") || normalized.contains("硕士") || normalized.contains("研究生")) {
            return MajorEducationLevel.GRADUATE.name();
        }
        if (normalized.contains("本科")) {
            return MajorEducationLevel.UNDERGRADUATE.name();
        }
        if (normalized.contains("专科") || normalized.contains("大专") || normalized.contains("高职")) {
            return MajorEducationLevel.VOCATIONAL.name();
        }
        // 兼容直接存枚举值的情况
        for (MajorEducationLevel level : MajorEducationLevel.values()) {
            if (level.name().equalsIgnoreCase(normalized)) {
                return level.name();
            }
        }
        return null;
    }

    // =============================================================
    // 目录管理基础查询（供 Controller 使用）
    // =============================================================

    /** 全部目录（含禁用，按 priority 升序） */
    public List<MajorCatalog> listCatalogs() {
        return majorCatalogMapper.selectAll();
    }

    /** 目录节点分页查询（keyword 模糊名称/精确代码，majorCode 精确，majorName 模糊） */
    public PageVO<MajorCatalogItemVO> pageItems(Long catalogId, String keyword, String majorCode,
                                                String majorName, int page, int size) {
        requireCatalog(catalogId);
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 200);

        long total = majorCatalogItemMapper.countItemsByCatalogId(catalogId, keyword, majorCode, majorName);
        List<MajorCatalogItemVO> items = total == 0
                ? new ArrayList<>()
                : majorCatalogItemMapper.selectItemsByCatalogId(catalogId, keyword, majorCode, majorName,
                        (safePage - 1) * safeSize, safeSize);
        return new PageVO<>(total, safePage, safeSize, items);
    }

    /** 跨目录专业检索：代码精确 / 名称模糊 / 别名精确，返回目录 + 节点 + 父级信息 */
    public List<MajorSearchItemVO> search(String keyword, int limit) {
        String trimmed = keyword == null ? "" : keyword.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "keyword 不能为空");
        }
        int safeLimit = Math.min(Math.max(1, limit), MAX_SEARCH_LIMIT);
        String normalized = MajorNameNormalizer.normalizeName(trimmed);
        return majorCatalogItemMapper.searchItems(trimmed, normalized, safeLimit);
    }

    private void requireCatalog(Long catalogId) {
        if (catalogId == null || majorCatalogMapper.selectById(catalogId) == null) {
            throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "专业目录不存在: id=" + catalogId);
        }
    }
}
