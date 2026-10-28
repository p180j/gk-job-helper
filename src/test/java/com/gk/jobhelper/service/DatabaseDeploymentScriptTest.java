package com.gk.jobhelper.service;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseDeploymentScriptTest {
    private static final Path DB_DIR = Paths.get("src/main/resources/db");
    private static final List<String> IMPORT_COLUMNS = Arrays.asList(
            "file_checksum", "parser_version", "processed_rows", "success_rows", "failed_rows", "error_message");
    private static final List<String> POSITION_COLUMNS = Arrays.asList(
            "department_code", "additional_requirements", "work_location", "settlement_location");

    @Test
    void containsOnlyFullAndIncrementalDatabaseScripts() throws IOException {
        List<Path> scripts;
        try (java.util.stream.Stream<Path> files = Files.list(DB_DIR)) {
            scripts = files.filter(path -> path.toString().endsWith(".sql")).collect(Collectors.toList());
        }
        assertEquals(Arrays.asList("hf_incremental.sql", "schema.sql"), scripts.stream()
                .map(path -> path.getFileName().toString()).sorted().collect(Collectors.toList()));
    }

    @Test
    void fullAndIncrementalScriptsContainCurrentImportSchema() throws IOException {
        String full = read(DB_DIR.resolve("schema.sql"));
        String incremental = read(DB_DIR.resolve("hf_incremental.sql"));
        for (String column : IMPORT_COLUMNS) {
            assertTrue(full.contains(column), "全量脚本缺少 import_file." + column);
            assertTrue(incremental.contains(column), "增量脚本缺少 import_file." + column);
        }
        for (String column : POSITION_COLUMNS) {
            assertTrue(full.contains(column), "全量脚本缺少 job_position." + column);
            assertTrue(incremental.contains(column), "增量脚本缺少 job_position." + column);
        }
        assertTrue(incremental.contains("information_schema.COLUMNS"));
        assertTrue(incremental.contains("information_schema.STATISTICS"));
        assertTrue(incremental.contains("SIGNAL SQLSTATE '45000'"));
    }

    @Test
    void incrementalScriptContainsAllHistoricalSchemaChanges() throws IOException {
        String incremental = read(DB_DIR.resolve("hf_incremental.sql"));
        for (String table : Arrays.asList("job_favorite", "job_interview_score", "job_position_feature",
                "job_preference", "career_profile", "career_fact", "resume_file", "recruitment_source",
                "recruitment_notice", "recruitment_attachment", "recruitment_position",
                "recruitment_requirement", "recruitment_match_result", "recruitment_match_item")) {
            assertTrue(incremental.contains("CREATE TABLE IF NOT EXISTS " + table), "统一增量脚本缺少表 " + table);
        }
        for (String column : Arrays.asList("english_level", "exam_year", "major_similarity_keys",
                "effective_major_requirement", "process_status", "dedupe_key", "parse_status",
                "source_type", "extraction_quality", "uncertain_reason")) {
            assertTrue(incremental.contains(column), "统一增量脚本缺少历史字段 " + column);
        }
        assertTrue(incremental.contains("JIANGXI_SASAC"));
        assertTrue(incremental.contains("JIANGXI_TALENT_EVALUATION"));
    }

    private String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
