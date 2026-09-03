package com.gk.jobhelper.matcher;

import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenderMatcherTest {

    private final GenderMatcher matcher = new GenderMatcher();

    @Test
    void structuredMaleRequirementShouldMatchMaleProfile() {
        assertEquals(MatchResult.MATCH, match("男", "男").getResult());
    }

    @Test
    void structuredMaleRequirementShouldRejectFemaleProfile() {
        MatchItemResult result = match("女", "男性");
        assertEquals(MatchResult.NOT_MATCH, result.getResult());
        assertTrue(result.getReason().contains("不符合"));
    }

    @Test
    void missingProfileGenderShouldRequireConfirmation() {
        assertEquals(MatchResult.UNCERTAIN, match(null, "女").getResult());
    }

    private MatchItemResult match(String gender, String requirement) {
        UserProfile profile = new UserProfile();
        profile.setGender(gender);
        JobPosition position = new JobPosition();
        position.setGenderRequirement(requirement);
        return matcher.match(profile, position, MatchContext.of(null));
    }
}
