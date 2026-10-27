package com.gk.jobhelper.service;

import com.gk.jobhelper.dto.ConversionResult;
import com.gk.jobhelper.dto.ExcelPreviewResult;
import com.gk.jobhelper.dto.ExcelRawSheet;
import com.gk.jobhelper.dto.FeatureRebuildResult;
import com.gk.jobhelper.dto.ImportConfirmRequest;
import com.gk.jobhelper.entity.ImportFile;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.mapper.ImportFileMapper;
import com.gk.jobhelper.mapper.JobPositionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/** Run with -Dgk.real.jiangxi=... -Dgk.real.national=...; never uses the live database. */
@SpringBootTest
@ActiveProfiles("test")
class RealWorkbookRegressionTest {
    @Autowired private ExcelPreviewParser previewParser;
    @Autowired private ExcelRowReader rowReader;
    @Autowired private FieldMappingService mappingService;
    @Autowired private JobPositionConverter converter;
    @Autowired private ImportFileMapper importFileMapper;
    @Autowired private JobPositionMapper positionMapper;
    @Autowired private JobFeatureService featureService;

    @Test
    void jiangxi2026() { verify("gk.real.jiangxi", 4409, false); }

    @Test
    void national2027() { verify("gk.real.national", 19426, true); }

    private void verify(String pathProperty, int expected, boolean hasLocations) {
        String path = System.getProperty(pathProperty);
        assumeTrue(path != null && new File(path).isFile(), "未指定真实 Excel 文件：" + pathProperty);
        File file = new File(path);
        ImportFile record = new ImportFile();
        record.setOriginalName(file.getName());
        record.setStoredName(file.getName());
        record.setStoredPath(file.getAbsolutePath());
        record.setFileSize(file.length());
        record.setFileType(".xls");
        record.setSheetName("真实回归");
        record.setHeaders("");
        record.setStatus("PREVIEWED");
        record.setCreatedAt(LocalDateTime.now());
        importFileMapper.insert(record);
        int converted = 0, other = 0, work = 0, settlement = 0;
        Set<String> naturalKeys = new HashSet<>();
        for (ExcelPreviewResult preview : previewParser.parseAll(file)) {
            if (preview.getParseError() != null) continue;
            ExcelRawSheet sheet = rowReader.read(file, preview.getSheetName());
            List<ImportConfirmRequest.MappingItem> mappings = new ArrayList<>();
            Set<String> mapped = new HashSet<>();
            for (String header : sheet.getHeaders()) {
                String target = mappingService.suggest(header).getSuggestedField();
                if (target == null || !mapped.add(target)) continue;
                ImportConfirmRequest.MappingItem item = new ImportConfirmRequest.MappingItem();
                item.setSourceField(header);
                item.setTargetField(target);
                mappings.add(item);
            }
            ConversionResult result = converter.convert(sheet, mappings, record.getId());
            assertTrue(result.getFailures().isEmpty(), preview.getSheetName() + " 存在转换失败行");
            for (JobPosition position : result.getPositions()) {
                if (position.getAdditionalRequirements() != null) other++;
                if (position.getWorkLocation() != null) work++;
                if (position.getSettlementLocation() != null) settlement++;
                assertTrue(naturalKeys.add(position.getDepartmentCode() + "|" + position.getPositionCode()));
                assertNotNull(position.getRawData());
                assertTrue(position.getSourceRow() > 0);
            }
            for (int start = 0; start < result.getPositions().size(); start += 200) {
                positionMapper.insertBatch(result.getPositions().subList(start, Math.min(start + 200, result.getPositions().size())));
            }
            converted += result.getPositions().size();
        }
        assertEquals(expected, converted);
        assertEquals(expected, positionMapper.countByImportFileId(record.getId()));
        FeatureRebuildResult preview = featureService.previewParsed(record.getId());
        assertEquals(expected, preview.getTotal());
        assertEquals(expected, preview.getChanged());
        assertEquals(expected, preview.getConfirmedLocations() + preview.getInferredLocations()
                + preview.getMultiSiteLocations() + preview.getUnknownLocations());
        System.out.println("REAL_FEATURE " + pathProperty + " changed=" + preview.getChanged()
                + " confirmed=" + preview.getConfirmedLocations() + " inferred=" + preview.getInferredLocations()
                + " multiSite=" + preview.getMultiSiteLocations() + " unknown=" + preview.getUnknownLocations());
        if (!hasLocations) {
            FeatureRebuildResult applied = featureService.applyParsed(record.getId());
            assertEquals(expected, applied.getChanged());
            assertEquals(0, featureService.previewParsed(record.getId()).getChanged());
        }
        if (hasLocations) {
            assertTrue(work > 0);
            assertTrue(settlement > 0);
            assertEquals(0, other);
        } else {
            assertTrue(other > 0);
            assertEquals(0, settlement);
        }
    }
}
