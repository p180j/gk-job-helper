package com.gk.jobhelper.service;

import com.gk.jobhelper.entity.RecruitmentPosition;
import org.springframework.stereotype.Component;

/** Keeps extraction confidence separate from qualification decisions. */
@Component
public class RecruitmentPositionQualityAssessor {
    public void assess(RecruitmentPosition position) {
        if (blank(position.getRawRequirement()) && blank(position.getOtherRequirement())) {
            position.setExtractionQuality("MANUAL_REQUIRED");
            return;
        }
        if (blank(position.getEducationRequirement()) || blank(position.getMajorRequirement()) || blank(position.getWorkYearsRequirement())) {
            position.setExtractionQuality("PARTIAL");
            return;
        }
        position.setExtractionQuality("COMPLETE");
    }

    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
