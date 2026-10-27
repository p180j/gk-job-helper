package com.gk.jobhelper.service;

import com.gk.jobhelper.dto.RecommendationCandidateRow;
import org.springframework.stereotype.Service;

import java.util.List;

/** Recommendation regions use the same derived work-site evidence as position details. */
@Service
public class JobRegionResolver {
    public String resolve(RecommendationCandidateRow row) {
        String status = row.getWorkLocationStatus();
        String name = row.getWorkLocationName();
        if ("CONFIRMED".equals(status)) return name;
        if ("INFERRED".equals(status)) return "行政辖区：" + name + "（工作地点待核实）";
        if ("MULTI_SITE".equals(status)) return "多个工作地点（具体地点待确定）";
        return "工作地点未知";
    }

    public boolean matches(List<String> preferredRegions, RecommendationCandidateRow row) {
        if (!"CONFIRMED".equals(row.getWorkLocationStatus()) || row.getWorkLocationName() == null
                || preferredRegions == null) return false;
        for (String preference : preferredRegions) {
            if (preference == null || preference.trim().isEmpty()) continue;
            String[] levels = preference.split("/");
            String target = levels[levels.length - 1].trim();
            if (target.isEmpty()) continue;
            if (row.getWorkLocationName().contains(target)) return true;
            String shortened = target.replaceFirst("(特别行政区|自治区|自治州|省|市|区|县)$", "");
            if (shortened.length() >= 2 && row.getWorkLocationName().contains(shortened)) return true;
        }
        return false;
    }
}
