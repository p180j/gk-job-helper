package com.gk.jobhelper.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.ArrayList;
import java.util.List;

/**
 * 正式导入请求（POST /api/import/{id}/confirm）
 * mappings 可在系统建议基础上人工修改；targetField 为空表示该列不导入
 */
public class ImportConfirmRequest {

    /** 指定导入的 Sheet 名称，为空时使用导入记录中的 Sheet */
    private String sheetName;

    /** 需合并导入的 Sheet 名称；为空时兼容旧流程，仅导入默认 Sheet。 */
    private List<String> sheetNames = new ArrayList<>();

    @NotEmpty(message = "字段映射不能为空")
    @Valid
    private List<MappingItem> mappings = new ArrayList<>();

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public List<String> getSheetNames() { return sheetNames; }
    public void setSheetNames(List<String> sheetNames) { this.sheetNames = sheetNames == null ? new ArrayList<String>() : sheetNames; }

    public List<MappingItem> getMappings() {
        return mappings;
    }

    public void setMappings(List<MappingItem> mappings) {
        this.mappings = mappings;
    }

    public static class MappingItem {

        @NotBlank(message = "sourceField 不能为空")
        private String sourceField;

        /** 目标标准字段英文名，null/空表示跳过该列 */
        private String targetField;

        public String getSourceField() {
            return sourceField;
        }

        public void setSourceField(String sourceField) {
            this.sourceField = sourceField;
        }

        public String getTargetField() {
            return targetField;
        }

        public void setTargetField(String targetField) {
            this.targetField = targetField;
        }
    }
}
