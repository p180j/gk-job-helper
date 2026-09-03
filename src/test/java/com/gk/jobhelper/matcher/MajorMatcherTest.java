package com.gk.jobhelper.matcher;

import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.ExamMajorCatalog;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.MajorCatalog;
import com.gk.jobhelper.entity.MajorCatalogItem;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.mapper.ExamMajorCatalogMapper;
import com.gk.jobhelper.mapper.MajorCatalogItemMapper;
import com.gk.jobhelper.mapper.MajorCatalogMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 专业资格匹配器测试（基于仓库内完整官方专业目录）。
 * 覆盖: 不限专业 / 名称精确 / 代码精确 / 专业类归属 / 多类别 OR / 相关专业 / 括号排除 /
 * 名称代码冲突 / 学历分段 / 目录优先级 / 同优先级目录冲突。
 * 事务回滚: 测试内插入的考试目录数据不污染其他测试。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MajorMatcherTest {

    private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 8, 27);
    private static final Long TEST_EXAM_ID = 9001L;
    private static final Long TEST_CONFLICT_EXAM_ID = 9002L;

    @Autowired
    private MajorMatcher majorMatcher;

    @Autowired
    private MajorCatalogMapper majorCatalogMapper;

    @Autowired
    private MajorCatalogItemMapper majorCatalogItemMapper;

    @Autowired
    private ExamMajorCatalogMapper examMajorCatalogMapper;

    // =============================================================
    // 1-2: 专业名称精确匹配
    // =============================================================

    @Test
    void exactMajorNameShouldMatch() {
        MatchItemResult result = match("软件工程", null, "本科", "软件工程", null);
        assertResult(result, MatchResult.MATCH);
        assertTrue(result.getReason().contains("软件工程"));
        assertEquals("软件工程", result.getUserValue());
        assertEquals("软件工程", result.getRequirementValue());
    }

    @Test
    void exactMajorNameWithMultipleCandidatesShouldMatch() {
        assertResult(match("软件工程", null, "本科", "软件工程、网络工程", null), MatchResult.MATCH);
        assertResult(match("软件工程", null, "本科", "软件工程，网络工程", null), MatchResult.MATCH);
        assertResult(match("软件工程", null, "本科", "软件工程/网络工程", null), MatchResult.MATCH);
        assertResult(match("软件工程", null, "本科", "软件工程；网络工程", null), MatchResult.MATCH);
        assertResult(match("网络工程", null, "本科", "软件工程、网络工程", null), MatchResult.MATCH);
    }

    @Test
    void nameSuffixAndWhitespaceShouldBeNormalized() {
        // "软件工程专业" 与 " 软件工程 " 均按比较值 "软件工程" 精确匹配
        assertResult(match("软件工程专业", null, "本科", "软件工程", null), MatchResult.MATCH);
        assertResult(match("软件工程", null, "本科", " 软件工程专业 ", null), MatchResult.MATCH);
    }

    @Test
    void partialNameShouldNotMatchByContains() {
        // "工程" 不能因 contains 匹配 "软件工程"；未收录专业 -> UNCERTAIN（绝非 MATCH）
        MatchItemResult result = match("工程", null, "本科", "软件工程", null);
        assertResult(result, MatchResult.UNCERTAIN);
        assertTrue(result.getReason().contains("未收录"));
    }

    // =============================================================
    // 3-5: 专业代码精确匹配
    // =============================================================

    @Test
    void majorCodeExactMatchShouldMatch() {
        MatchItemResult result = match("软件工程", "080902", "本科", "080902", null);
        assertResult(result, MatchResult.MATCH);
        assertEquals("080902", result.getEvidence().getMajorCode());
    }

    @Test
    void majorCodeWithNameShouldMatch() {
        MatchItemResult result = match("软件工程", "080902", "本科", "080902 软件工程", null);
        assertResult(result, MatchResult.MATCH);
    }

    // =============================================================
    // 4-6: 专业类匹配（目录父子树验证）
    // =============================================================

    @Test
    void majorBelongingToClassShouldMatch() {
        MatchItemResult result = match("软件工程", null, "本科", "计算机类", null);
        assertResult(result, MatchResult.MATCH);
        assertNotNull(result.getEvidence());
        assertEquals("MOE_UNDERGRADUATE_2024", result.getEvidence().getCatalogCode());
        assertEquals("080902", result.getEvidence().getMajorCode());
        assertEquals("软件工程", result.getEvidence().getMajorName());
        assertEquals("0809", result.getEvidence().getParentCode());
        assertEquals("计算机类", result.getEvidence().getParentName());
        assertTrue(result.getReason().contains("计算机类(0809)"));
    }

    @Test
    void majorCodeWithClassRequirementShouldMatch() {
        MatchItemResult result = match("软件工程", "080902", "本科", "计算机类", null);
        assertResult(result, MatchResult.MATCH);
        assertEquals("0809", result.getEvidence().getParentCode());
    }

    @Test
    void classCodeRequirementShouldMatch() {
        MatchItemResult result = match("软件工程", "080902", "本科", "0809计算机类", null);
        assertResult(result, MatchResult.MATCH);
        assertEquals("0809", result.getEvidence().getParentCode());
    }

    @Test
    void categoryLevelRequirementShouldMatch() {
        // "08 工学" 学科门类同样通过目录祖先链匹配
        MatchItemResult result = match("软件工程", "080902", "本科", "工学", null);
        assertResult(result, MatchResult.MATCH);
        assertEquals("08", result.getEvidence().getParentCode());
    }

    @Test
    void officialMajorShouldResolveToClass() {
        // 官方专业 "计算机科学与技术" -> 属于计算机类
        MatchItemResult result = match("计算机科学与技术", null, "本科", "计算机类", null);
        assertResult(result, MatchResult.MATCH);
        assertEquals("080901", result.getEvidence().getMajorCode());
    }

    // =============================================================
    // 7: 跨类明确不匹配
    // =============================================================

    @Test
    void majorNotBelongingToRecognizedClassShouldNotMatch() {
        MatchItemResult result = match("软件工程", "080902", "本科", "法学类", null);
        assertResult(result, MatchResult.NOT_MATCH);
        assertTrue(result.getReason().contains("无归属关系"));
    }

    // =============================================================
    // 8-9: 相关专业规则
    // =============================================================

    @Test
    void relatedMajorExpressionShouldBeUncertain() {
        MatchItemResult result = match("软件工程", "080902", "本科", "计算机相关专业", null);
        assertResult(result, MatchResult.UNCERTAIN);
        assertTrue(result.getReason().contains("非标准范围表述"));
    }

    @Test
    void relatedSuffixWithStandardMatchShouldMatch() {
        // 用户明确属于计算机类: "计算机类及相关专业" -> MATCH（不能因"相关专业"降级为 UNCERTAIN）
        MatchItemResult result = match("软件工程", "080902", "本科", "计算机类及相关专业", null);
        assertResult(result, MatchResult.MATCH);
    }

    @Test
    void relatedSuffixWithoutStandardMatchShouldBeUncertain() {
        // 用户明确不属于法学类，但"相关专业"部分无法可靠判断 -> UNCERTAIN
        MatchItemResult result = match("软件工程", "080902", "本科", "法学类及相关专业", null);
        assertResult(result, MatchResult.UNCERTAIN);
    }

    // =============================================================
    // 10: 未知专业
    // =============================================================

    @Test
    void unknownUserMajorShouldBeUncertain() {
        MatchItemResult result = match("未收录测试专业", null, "本科", "计算机类", null);
        assertResult(result, MatchResult.UNCERTAIN);
        assertTrue(result.getReason().contains("未收录"));
    }

    // =============================================================
    // 11-12: 括号排除
    // =============================================================

    @Test
    void excludedMajorShouldNotMatch() {
        MatchItemResult result = match("数字媒体技术", null, "本科", "计算机类（不含数字媒体技术）", null);
        assertResult(result, MatchResult.NOT_MATCH);
        assertTrue(result.getReason().contains("排除"));
    }

    @Test
    void nonExcludedMajorUnderClassShouldMatch() {
        MatchItemResult result = match("软件工程", null, "本科", "计算机类（不含数字媒体技术）", null);
        assertResult(result, MatchResult.MATCH);
    }

    @Test
    void unparseableParenthesisConstraintShouldBeUncertain() {
        // 括号内非排除、非代码的限定内容无法可靠解析 -> UNCERTAIN（不静默丢弃）
        MatchItemResult result = match("软件工程", null, "本科", "计算机类（软件工程限工学学位）", null);
        assertResult(result, MatchResult.UNCERTAIN);
        assertTrue(result.getReason().contains("括号限定"));
    }

    // =============================================================
    // 13: 名称与代码冲突
    // =============================================================

    @Test
    void majorNameCodeConflictShouldBeUncertain() {
        MatchItemResult result = match("软件工程", "080901", "本科", "计算机类", null);
        assertResult(result, MatchResult.UNCERTAIN);
        assertTrue(result.getReason().contains("冲突"));
        assertTrue(result.getReason().contains("人工确认"));
    }

    // =============================================================
    // 14-16: 不限 / 空值
    // =============================================================

    @Test
    void unlimitedMajorShouldMatch() {
        assertResult(match("软件工程", "080902", "本科", "不限", null), MatchResult.MATCH);
        assertResult(match("软件工程", "080902", "本科", "专业不限", null), MatchResult.MATCH);
        assertResult(match("软件工程", "080902", "本科", "不限专业", null), MatchResult.MATCH);
        assertResult(match("软件工程", "080902", "本科", "无专业限制", null), MatchResult.MATCH);
        assertResult(match("软件工程", "080902", "本科", "无要求", null), MatchResult.MATCH);
        assertEquals("岗位不限专业。",
                match("软件工程", "080902", "本科", "专业不限", null).getReason());
    }

    @Test
    void emptyRequirementShouldMeanNoRestriction() {
        MatchItemResult result = match("软件工程", "080902", "本科", null, null);
        assertResult(result, MatchResult.MATCH);
        assertTrue(result.getReason().contains("不限专业"));
    }

    @Test
    void emptyUserMajorShouldBeUncertain() {
        MatchItemResult result = match(null, null, "本科", "计算机类", null);
        assertResult(result, MatchResult.UNCERTAIN);
        assertTrue(result.getReason().contains("为空"));
    }

    // =============================================================
    // 17: 学历层级分段
    // =============================================================

    @Test
    void leveledRequirementShouldSelectUserLevelSegment() {
        MatchItemResult result = match("软件工程", "080902", "本科", "本科：计算机类；研究生：法学", null);
        assertResult(result, MatchResult.MATCH);
    }

    @Test
    void leveledRequirementWithProfessionalSuffixShouldSelectUserLevelSegment() {
        MatchItemResult result = match("软件工程", "080902", "本科",
                "研究生专业：计算机类；本科专业：法学类", null);
        assertResult(result, MatchResult.NOT_MATCH);
    }

    @Test
    void leveledRequirementWithoutMatchingLevelShouldBeUncertain() {
        // 用户专科，要求仅分本科/研究生段 -> 无法选择适用段 -> UNCERTAIN
        MatchItemResult result = match("软件工程", "080902", "大专", "本科：计算机类；研究生：法学", null);
        assertResult(result, MatchResult.UNCERTAIN);
    }

    // =============================================================
    // 18: 专业代码不存在
    // =============================================================

    @Test
    void nonExistingMajorCodeShouldBeUncertain() {
        MatchItemResult result = match("软件工程", "999999", "本科", "计算机类", null);
        assertResult(result, MatchResult.UNCERTAIN);
        assertTrue(result.getReason().contains("999999"));
        assertTrue(result.getReason().contains("不存在"));
    }

    // =============================================================
    // 研究生目录
    // =============================================================

    @Test
    void graduateDisciplineShouldMatchInGraduateCatalog() {
        MatchItemResult result = match("计算机科学与技术（可授工学、理学学位）", "0812", "硕士研究生", "0812", null);
        assertResult(result, MatchResult.MATCH);
        assertEquals("MOE_GRADUATE_2022", result.getEvidence().getCatalogCode());
        assertEquals("0812", result.getEvidence().getMajorCode());
    }

    @Test
    void undergraduateClassRequirementForGraduateShouldBeUncertain() {
        // 研究生一级学科与本科专业类不是同一层级概念：研究生目录无"计算机类" -> UNCERTAIN
        MatchItemResult result = match("软件工程", null, "硕士研究生", "计算机类", null);
        assertResult(result, MatchResult.UNCERTAIN);
    }

    // =============================================================
    // 多类别 OR / UNCERTAIN 聚合
    // =============================================================

    @Test
    void multipleClassesWithAnyMatchShouldMatch() {
        assertResult(match("软件工程", "080902", "本科", "计算机类、法学类", null), MatchResult.MATCH);
    }

    @Test
    void multipleClassesAllRecognizedWithoutMatchShouldNotMatch() {
        // 用户专业与两个类别均明确收录且无归属关系 -> NOT_MATCH
        MatchItemResult result = match("软件工程", "080902", "本科", "法学类、法学", null);
        assertResult(result, MatchResult.NOT_MATCH);
    }

    @Test
    void multipleClassesWithUnrecognizedOneShouldBeUncertain() {
        // 用户明确不属于已识别的法学类，但另一专业类未收录 -> 不能直接 NOT_MATCH -> UNCERTAIN
        MatchItemResult result = match("软件工程", "080902", "本科", "法学类、未收录测试专业类", null);
        assertResult(result, MatchResult.UNCERTAIN);
        assertTrue(result.getReason().contains("未收录"));
    }

    @Test
    void recognizedCombinedCategoryShouldUseItsOfficialMembers() {
        // “财会审计类”不是官方目录单一节点，但可明确映射到会计学、财务管理、审计学。
        assertResult(match("会计学", "120203K", "本科", "财会审计类", null), MatchResult.MATCH);
        assertResult(match("软件工程", "080902", "本科", "财政学类、财会审计类", null), MatchResult.NOT_MATCH);
    }

    @Test
    void unrecognizedCombinedCategoryShouldRemainUncertain() {
        assertResult(match("软件工程", "080902", "本科", "未收录测试组合类", null), MatchResult.UNCERTAIN);
    }

    // =============================================================
    // 无可用目录
    // =============================================================

    @Test
    void noAvailableCatalogShouldBeUncertain() {
        // 中专层级无对应目录且岗位无考试绑定 -> 无可用目录
        MatchItemResult result = match("软件工程", null, "中专", "计算机类", null);
        assertResult(result, MatchResult.UNCERTAIN);
        assertTrue(result.getReason().contains("无可用专业目录"));
    }

    // =============================================================
    // 目录优先级: 考试绑定目录优先，且高优先级结果不被低优先级推翻
    // =============================================================

    @Test
    void examCatalogPriorityShouldOverrideMoe() {
        // 考试目录: 软件工程属于电子信息类（不属于计算机类）-> NOT_MATCH；
        // 教育部目录: 软件工程属于计算机类 -> MATCH；考试目录优先级更高 -> NOT_MATCH 生效
        insertExamCatalog(TEST_EXAM_ID, "电子信息类", 10);

        MatchItemResult result = match("软件工程", "080902", "本科", "计算机类", TEST_EXAM_ID);
        assertResult(result, MatchResult.NOT_MATCH);
    }

    @Test
    void examCatalogPositiveMatchShouldCarryExamCatalogEvidence() {
        // 考试目录: 软件工程属于计算机类 -> MATCH，证据目录为考试目录
        insertExamCatalog(TEST_EXAM_ID, "计算机类", 10);

        MatchItemResult result = match("软件工程", "080902", "本科", "计算机类", TEST_EXAM_ID);
        assertResult(result, MatchResult.MATCH);
        assertNotNull(result.getEvidence());
        assertTrue(result.getEvidence().getCatalogCode().startsWith("EXAM_TEST"));
    }

    // =============================================================
    // 同优先级目录冲突
    // =============================================================

    @Test
    void sameTierCatalogConflictShouldBeUncertain() {
        // 两个同优先级考试目录: 一个判定 MATCH、一个判定 NOT_MATCH -> UNCERTAIN
        insertExamCatalog(TEST_CONFLICT_EXAM_ID, "计算机类", 10);
        insertExamCatalog(TEST_CONFLICT_EXAM_ID, "电子信息类", 10);

        MatchItemResult result = match("软件工程", "080902", "本科", "计算机类", TEST_CONFLICT_EXAM_ID);
        assertResult(result, MatchResult.UNCERTAIN);
        assertTrue(result.getReason().contains("冲突"));
        // 必须保留使用了哪些目录的信息
        assertTrue(result.getReason().contains("考试目录"));
    }

    // =============================================================
    // 工具
    // =============================================================

    private MatchItemResult match(String major, String majorCode, String education,
                                  String requirement, Long examId) {
        UserProfile profile = new UserProfile();
        profile.setName("测试用户");
        profile.setEducation(education);
        profile.setMajor(major);
        profile.setMajorCode(majorCode);
        JobPosition position = new JobPosition();
        position.setPositionName("测试岗位");
        position.setMajorRequirement(requirement);
        position.setExamId(examId);
        return majorMatcher.match(profile, position, MatchContext.of(REFERENCE_DATE));
    }

    private void assertResult(MatchItemResult result, MatchResult expected) {
        assertNotNull(result);
        assertEquals(expected, result.getResult());
    }

    /**
     * 插入考试专用目录（EXAM 类型）并绑定到指定考试:
     * 计算机类(0809) + 电子信息类(0807) + 软件工程(080902)，
     * softwareParent 指定软件工程挂到哪个类下（用于构造 MATCH / NOT_MATCH / 冲突场景）。
     */
    private void insertExamCatalog(Long examId, String softwareParent, int bindingPriority) {
        LocalDateTime now = LocalDateTime.now();

        MajorCatalog catalog = new MajorCatalog();
        catalog.setCatalogCode("EXAM_TEST_" + System.nanoTime());
        catalog.setCatalogName("税务系统考试目录");
        catalog.setCatalogType("EXAM");
        catalog.setEducationLevel("UNDERGRADUATE");
        catalog.setVersion("2026");
        catalog.setSourceName("测试招录单位");
        catalog.setPriority(50);
        catalog.setEnabled(true);
        catalog.setCreatedAt(now);
        catalog.setUpdatedAt(now);
        majorCatalogMapper.insert(catalog);

        MajorCatalogItem csClass = insertItem(catalog.getId(), null, "0809", "计算机类", "CLASS", 1, now);
        MajorCatalogItem ictClass = insertItem(catalog.getId(), null, "0807", "电子信息类", "CLASS", 2, now);
        Long parentId = "计算机类".equals(softwareParent) ? csClass.getId() : ictClass.getId();
        insertItem(catalog.getId(), parentId, "080902", "软件工程", "MAJOR", 1, now);

        ExamMajorCatalog binding = new ExamMajorCatalog();
        binding.setExamId(examId);
        binding.setCatalogId(catalog.getId());
        binding.setPriority(bindingPriority);
        binding.setCreatedAt(now);
        examMajorCatalogMapper.insert(binding);
    }

    private MajorCatalogItem insertItem(Long catalogId, Long parentId, String code, String name,
                                        String itemLevel, int sortNo, LocalDateTime now) {
        MajorCatalogItem item = new MajorCatalogItem();
        item.setCatalogId(catalogId);
        item.setParentId(parentId);
        item.setMajorCode(code);
        item.setMajorName(name);
        item.setNormalizedName(name);
        item.setItemLevel(itemLevel);
        item.setSortNo(sortNo);
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        majorCatalogItemMapper.insert(item);
        return item;
    }
}
