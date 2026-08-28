package com.gk.jobhelper.service;

import com.gk.jobhelper.entity.ExamMajorCatalog;
import com.gk.jobhelper.entity.MajorCatalog;
import com.gk.jobhelper.entity.MajorCatalogItem;
import com.gk.jobhelper.mapper.ExamMajorCatalogMapper;
import com.gk.jobhelper.mapper.MajorCatalogMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 专业目录服务测试（基于内置官方样例数据）:
 * 父子目录关系 / 按代码查询 / 按名称查询 / 按别名查询 / 查询祖先 / 查询后代 /
 * exam 目录优先级 / 目录启停。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MajorCatalogServiceTest {

    private static final Long UNDERGRADUATE_CATALOG_ID = 1L;
    private static final Long GRADUATE_CATALOG_ID = 2L;

    @Autowired
    private MajorCatalogService majorCatalogService;

    @Autowired
    private MajorCatalogMapper majorCatalogMapper;

    @Autowired
    private ExamMajorCatalogMapper examMajorCatalogMapper;

    // =============================================================
    // 父子目录关系
    // =============================================================

    @Test
    void ancestorsOfMajorShouldWalkParentChain() {
        MajorCatalog catalog = catalog(UNDERGRADUATE_CATALOG_ID);
        MajorCatalogItem software = majorCatalogService.findByCode(catalog, "080902");
        assertNotNull(software);

        List<MajorCatalogItem> ancestors = majorCatalogService.getAncestors(software);
        assertEquals(2, ancestors.size());
        assertEquals("0809", ancestors.get(0).getMajorCode());
        assertEquals("计算机类", ancestors.get(0).getMajorName());
        assertEquals("08", ancestors.get(1).getMajorCode());
        assertEquals("工学", ancestors.get(1).getMajorName());
    }

    @Test
    void ancestorsOfRootShouldBeEmpty() {
        MajorCatalog catalog = catalog(UNDERGRADUATE_CATALOG_ID);
        MajorCatalogItem engineering = majorCatalogService.findByCode(catalog, "08");
        assertNotNull(engineering);
        assertTrue(majorCatalogService.getAncestors(engineering).isEmpty());
    }

    @Test
    void descendantsOfClassShouldIncludeAllMajors() {
        MajorCatalog catalog = catalog(UNDERGRADUATE_CATALOG_ID);
        MajorCatalogItem csClass = majorCatalogService.findByCode(catalog, "0809");
        assertNotNull(csClass);

        List<MajorCatalogItem> descendants = majorCatalogService.getDescendants(csClass);
        assertEquals(15, descendants.size());
        boolean hasSoftware = false;
        for (MajorCatalogItem item : descendants) {
            if ("080902".equals(item.getMajorCode())) {
                hasSoftware = true;
            }
        }
        assertTrue(hasSoftware);
    }

    @Test
    void isAncestorOrSelfShouldJudgeByParentChain() {
        MajorCatalog catalog = catalog(UNDERGRADUATE_CATALOG_ID);
        MajorCatalogItem csClass = majorCatalogService.findByCode(catalog, "0809");
        MajorCatalogItem software = majorCatalogService.findByCode(catalog, "080902");
        MajorCatalogItem lawClass = majorCatalogService.findByCode(catalog, "0301");

        assertTrue(majorCatalogService.isAncestorOrSelf(csClass, software));
        assertTrue(majorCatalogService.isAncestorOrSelf(software, software));
        // 代码前缀相同不代表目录父子关系: 法学类 0301 不是 软件工程 080902 的祖先
        assertTrue(!majorCatalogService.isAncestorOrSelf(lawClass, software));
    }

    // =============================================================
    // 按代码查询
    // =============================================================

    @Test
    void findByCodeShouldNormalizeCode() {
        MajorCatalog catalog = catalog(UNDERGRADUATE_CATALOG_ID);
        assertEquals("软件工程", majorCatalogService.findByCode(catalog, "080902").getMajorName());
        // 全角数字 + 空格
        assertNotNull(majorCatalogService.findByCode(catalog, "０８０９０２"));
        assertEquals("计算机类", majorCatalogService.findByCode(catalog, " 0809 ").getMajorName());
        assertNull(majorCatalogService.findByCode(catalog, "999999"));
        assertNull(majorCatalogService.findByCode(catalog, null));
    }

    // =============================================================
    // 按名称 / 别名查询
    // =============================================================

    @Test
    void findByNameShouldMatchExactly() {
        MajorCatalog catalog = catalog(UNDERGRADUATE_CATALOG_ID);

        List<MajorCatalogItem> items = majorCatalogService.findByName(catalog, "软件工程");
        assertEquals(1, items.size());
        assertEquals("080902", items.get(0).getMajorCode());

        // "专业"后缀仅用于比较
        assertEquals("080902", majorCatalogService.findByName(catalog, "软件工程专业").get(0).getMajorCode());
        // 首尾空白
        assertEquals("080902", majorCatalogService.findByName(catalog, " 软件工程 ").get(0).getMajorCode());
        // 名称精确匹配: "软件" 不能 contains 命中 "软件工程"
        assertTrue(majorCatalogService.findByName(catalog, "软件").isEmpty());
        assertTrue(majorCatalogService.findByName(catalog, "计算机").isEmpty());
    }

    @Test
    void findByAliasShouldResolveToOfficialMajor() {
        MajorCatalog catalog = catalog(UNDERGRADUATE_CATALOG_ID);
        List<MajorCatalogItem> items = majorCatalogService.findByName(catalog, "计算机科学技术");
        assertEquals(1, items.size());
        assertEquals("080901", items.get(0).getMajorCode());
        assertEquals("计算机科学与技术", items.get(0).getMajorName());
    }

    @Test
    void findByNameInGraduateCatalogShouldWork() {
        MajorCatalog catalog = catalog(GRADUATE_CATALOG_ID);
        assertEquals("0835", majorCatalogService.findByName(catalog, "软件工程").get(0).getMajorCode());
        assertEquals("0812", majorCatalogService.findByCode(catalog, "0812").getMajorCode());
        // 研究生一级学科与本科专业类不是同一层级概念
        assertTrue(majorCatalogService.findByName(catalog, "计算机类").isEmpty());
    }

    // =============================================================
    // 目录优先级
    // =============================================================

    @Test
    void resolveCatalogsShouldReturnMoeCatalogByLevel() {
        List<MajorCatalogService.CatalogEntry> undergraduate =
                majorCatalogService.resolveCatalogs(null, "UNDERGRADUATE");
        assertEquals(1, undergraduate.size());
        assertEquals("MOE_UNDERGRADUATE_2026", undergraduate.get(0).getCatalog().getCatalogCode());

        List<MajorCatalogService.CatalogEntry> graduate =
                majorCatalogService.resolveCatalogs(null, "GRADUATE");
        assertEquals(1, graduate.size());
        assertEquals("MOE_GRADUATE_2022", graduate.get(0).getCatalog().getCatalogCode());

        // 无考试绑定且无法确定学历层级 -> 空目录链
        assertTrue(majorCatalogService.resolveCatalogs(null, null).isEmpty());
    }

    @Test
    void examBindingCatalogShouldTakePriorityOverMoe() {
        Long examCatalogId = insertExamCatalog(true);
        insertBinding(9100L, examCatalogId, 10);

        List<MajorCatalogService.CatalogEntry> entries =
                majorCatalogService.resolveCatalogs(9100L, "UNDERGRADUATE");
        assertEquals(2, entries.size());
        // 考试绑定目录优先
        assertEquals(examCatalogId, entries.get(0).getCatalog().getId());
        assertEquals(10, entries.get(0).getTier());
        // 教育部目录其次（tier 恒大于考试绑定优先级）
        assertEquals(UNDERGRADUATE_CATALOG_ID, entries.get(1).getCatalog().getId());
        assertTrue(entries.get(1).getTier() > entries.get(0).getTier());
    }

    @Test
    void disabledCatalogShouldBeExcluded() {
        Long examCatalogId = insertExamCatalog(false);
        insertBinding(9101L, examCatalogId, 10);

        List<MajorCatalogService.CatalogEntry> entries =
                majorCatalogService.resolveCatalogs(9101L, "UNDERGRADUATE");
        assertEquals(1, entries.size());
        assertEquals(UNDERGRADUATE_CATALOG_ID, entries.get(0).getCatalog().getId());
    }

    // =============================================================
    // 学历层级推断
    // =============================================================

    @Test
    void resolveEducationLevelShouldMapCorrectly() {
        assertEquals("GRADUATE", MajorCatalogService.resolveEducationLevel("研究生", null));
        assertEquals("GRADUATE", MajorCatalogService.resolveEducationLevel(null, "硕士研究生"));
        assertEquals("GRADUATE", MajorCatalogService.resolveEducationLevel(null, "博士"));
        assertEquals("UNDERGRADUATE", MajorCatalogService.resolveEducationLevel("本科", "大专"));
        assertEquals("VOCATIONAL", MajorCatalogService.resolveEducationLevel(null, "大专"));
        assertEquals("VOCATIONAL", MajorCatalogService.resolveEducationLevel(null, "高职"));
        assertNull(MajorCatalogService.resolveEducationLevel(null, null));
        assertNull(MajorCatalogService.resolveEducationLevel(null, "高中"));
    }

    // =============================================================
    // 工具
    // =============================================================

    private MajorCatalog catalog(Long id) {
        MajorCatalog catalog = majorCatalogMapper.selectById(id);
        assertNotNull(catalog);
        return catalog;
    }

    private Long insertExamCatalog(boolean enabled) {
        LocalDateTime now = LocalDateTime.now();
        MajorCatalog catalog = new MajorCatalog();
        catalog.setCatalogCode("EXAM_SVC_TEST_" + System.nanoTime());
        catalog.setCatalogName("测试考试目录");
        catalog.setCatalogType("EXAM");
        catalog.setEducationLevel("UNDERGRADUATE");
        catalog.setVersion("2026");
        catalog.setSourceName("测试");
        catalog.setPriority(50);
        catalog.setEnabled(enabled);
        catalog.setCreatedAt(now);
        catalog.setUpdatedAt(now);
        majorCatalogMapper.insert(catalog);
        return catalog.getId();
    }

    private void insertBinding(Long examId, Long catalogId, int priority) {
        ExamMajorCatalog binding = new ExamMajorCatalog();
        binding.setExamId(examId);
        binding.setCatalogId(catalogId);
        binding.setPriority(priority);
        binding.setCreatedAt(LocalDateTime.now());
        examMajorCatalogMapper.insert(binding);
    }
}
