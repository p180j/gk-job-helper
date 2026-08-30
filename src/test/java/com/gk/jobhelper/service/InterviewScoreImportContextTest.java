package com.gk.jobhelper.service;

import com.gk.jobhelper.dto.InterviewScoreImportResult;
import com.gk.jobhelper.entity.JobInterviewScore;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.mapper.JobInterviewScoreMapper;
import com.gk.jobhelper.mapper.JobPositionMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewScoreImportContextTest {
    @Test
    void reportsInsertUpdateAndLinksOnlyAgainstCurrentImport() throws Exception {
        JobInterviewScoreMapper scores = mock(JobInterviewScoreMapper.class);
        JobPositionMapper positions = mock(JobPositionMapper.class);
        JobInterviewScore existing = new JobInterviewScore();
        existing.setPositionCode("A001");
        when(scores.selectByYearAndCodes(2026, Arrays.asList("A001", "B001")))
                .thenReturn(Collections.singletonList(existing));
        JobPosition linked = new JobPosition();
        linked.setPositionCode("A001");
        when(positions.selectByImportFileIdAndPositionCodes(9L, Arrays.asList("A001", "B001")))
                .thenReturn(Collections.singletonList(linked));

        InterviewScoreImportService service = new InterviewScoreImportService(new ExcelRowReader(), scores, positions);
        Path file = workbook();
        try {
            InterviewScoreImportResult result = service.importFile(2026, 9L, file.toFile(), "second-file.xlsx");
            assertEquals(1, result.getInsertedCount());
            assertEquals(1, result.getUpdatedCount());
            assertEquals(1, result.getLinkedPositionCount());
            assertEquals(1, result.getUnlinkedPositionCount());
            verify(positions).selectByImportFileIdAndPositionCodes(9L, Arrays.asList("A001", "B001"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void checksExistingScoresWithinTheRequestedYear() throws Exception {
        JobInterviewScoreMapper scores = mock(JobInterviewScoreMapper.class);
        JobPositionMapper positions = mock(JobPositionMapper.class);
        when(scores.selectByYearAndCodes(2025, Arrays.asList("A001", "B001")))
                .thenReturn(Collections.emptyList());
        when(positions.selectByImportFileIdAndPositionCodes(eq(10L), anyList()))
                .thenReturn(Collections.emptyList());

        InterviewScoreImportService service = new InterviewScoreImportService(new ExcelRowReader(), scores, positions);
        Path file = workbook();
        try {
            InterviewScoreImportResult result = service.importFile(2025, 10L, file.toFile(), "2025-file.xlsx");
            assertEquals(2, result.getInsertedCount());
            assertEquals(0, result.getUpdatedCount());
            verify(scores).selectByYearAndCodes(2025, Arrays.asList("A001", "B001"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private Path workbook() throws Exception {
        Path file = Files.createTempFile("interview-context-", ".xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("名单");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("职位代码");
            header.createCell(1).setCellValue("进入面试最低分");
            row(sheet, 1, "A001", "130.5");
            row(sheet, 2, "B001", "128.0");
            try (OutputStream output = Files.newOutputStream(file)) {
                workbook.write(output);
            }
        }
        return file;
    }

    private void row(Sheet sheet, int index, String code, String score) {
        Row row = sheet.createRow(index);
        row.createCell(0).setCellValue(code);
        row.createCell(1).setCellValue(score);
    }
}
