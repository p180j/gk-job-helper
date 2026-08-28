package com.gk.jobhelper.service;

import com.gk.jobhelper.common.MajorNameNormalizer;
import com.gk.jobhelper.entity.MajorCatalog;
import com.gk.jobhelper.entity.MajorCatalogItem;
import com.gk.jobhelper.mapper.MajorAliasMapper;
import com.gk.jobhelper.mapper.MajorCatalogItemMapper;
import com.gk.jobhelper.mapper.MajorCatalogMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 将仓库内的教育部官方目录 CSV 离线导入数据库。应用运行和测试均不依赖网络。
 * 目录文件由 tools/generate_major_catalog.py 从 docs 中记载的官方原件生成。
 */
@Component
@Profile("!test")
public class MajorCatalogCsvImporter {

    private final MajorCatalogMapper catalogMapper;
    private final MajorCatalogItemMapper itemMapper;
    private final MajorAliasMapper aliasMapper;

    public MajorCatalogCsvImporter(MajorCatalogMapper catalogMapper, MajorCatalogItemMapper itemMapper,
                                   MajorAliasMapper aliasMapper) {
        this.catalogMapper = catalogMapper;
        this.itemMapper = itemMapper;
        this.aliasMapper = aliasMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void importOfficialCatalogs() {
        importIfNeeded(new CatalogDefinition("MOE_UNDERGRADUATE_2024", "普通高等学校本科专业目录（2024年）",
                "UNDERGRADUATE", "2024", "https://www.moe.gov.cn/srcsite/A08/moe_1034/s4930/202403/W020240319305498791768.pdf",
                "major-catalog/undergraduate-2024.csv", 917, 10));
        importIfNeeded(new CatalogDefinition("MOE_GRADUATE_2022", "研究生教育学科专业目录（2022年）",
                "GRADUATE", "2022", "https://www.moe.gov.cn/srcsite/A22/moe_833/202209/W020220914572994461110.pdf",
                "major-catalog/graduate-2022.csv", 195, 10));
        importIfNeeded(new CatalogDefinition("MOE_VOCATIONAL_2021", "职业教育专业目录（2021年）",
                "VOCATIONAL", "2021", "https://www.moe.gov.cn/srcsite/A07/moe_953/202103/W020210319595911145604.docx",
                "major-catalog/vocational-2021.csv", 1681, 10));
    }

    private void importIfNeeded(CatalogDefinition definition) {
        MajorCatalog catalog = catalogMapper.selectByCode(definition.catalogCode);
        if (catalog == null) {
            catalog = new MajorCatalog();
            catalog.setCatalogCode(definition.catalogCode);
            catalog.setCatalogName(definition.catalogName);
            catalog.setCatalogType("MOE");
            catalog.setEducationLevel(definition.educationLevel);
            catalog.setVersion(definition.version);
            catalog.setSourceName("中华人民共和国教育部");
            catalog.setSourceUrl(definition.sourceUrl);
            catalog.setSourceYear(definition.version);
            catalog.setPriority(definition.priority);
            catalog.setEnabled(true);
            catalog.setCreatedAt(LocalDateTime.now());
            catalog.setUpdatedAt(LocalDateTime.now());
            catalogMapper.insert(catalog);
        }
        if (itemMapper.countByCatalogId(catalog.getId()) >= definition.expectedNodes) {
            return;
        }
        aliasMapper.deleteByCatalogId(catalog.getId());
        itemMapper.deleteByCatalogId(catalog.getId());
        Map<String, Long> ids = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ClassPathResource(definition.resource).getInputStream(), StandardCharsets.UTF_8))) {
            reader.readLine();
            String line;
            int sortNo = 0;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",", -1);
                if (values.length != 11) {
                    throw new IllegalStateException("专业目录 CSV 列数错误: " + definition.resource);
                }
                MajorCatalogItem item = new MajorCatalogItem();
                item.setCatalogId(catalog.getId());
                item.setParentId(ids.get(values[1] + ":" + values[8]));
                item.setMajorCode(values[6]);
                item.setMajorName(values[7]);
                item.setNormalizedName(MajorNameNormalizer.comparisonName(values[7]));
                item.setItemLevel(values[9]);
                item.setDegreeCategory("FIELD".equals(values[9]) ? "专业学位" : null);
                item.setSortNo(++sortNo);
                item.setRawData(line);
                item.setCreatedAt(LocalDateTime.now());
                item.setUpdatedAt(LocalDateTime.now());
                itemMapper.insert(item);
                ids.put(values[1] + ":" + values[6], item.getId());
            }
        } catch (Exception e) {
            throw new IllegalStateException("读取官方专业目录失败: " + definition.resource, e);
        }
    }

    private static class CatalogDefinition {
        private final String catalogCode;
        private final String catalogName;
        private final String educationLevel;
        private final String version;
        private final String sourceUrl;
        private final String resource;
        private final int expectedNodes;
        private final int priority;

        private CatalogDefinition(String catalogCode, String catalogName, String educationLevel, String version,
                                  String sourceUrl, String resource, int expectedNodes, int priority) {
            this.catalogCode = catalogCode;
            this.catalogName = catalogName;
            this.educationLevel = educationLevel;
            this.version = version;
            this.sourceUrl = sourceUrl;
            this.resource = resource;
            this.expectedNodes = expectedNodes;
            this.priority = priority;
        }
    }
}
