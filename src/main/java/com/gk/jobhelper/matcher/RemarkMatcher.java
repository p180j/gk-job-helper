package com.gk.jobhelper.matcher;

import com.gk.jobhelper.common.TextNormalizer;
import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 备注补充条件匹配器。
 *
 * 备注是非结构化文本：对可可靠识别的毕业届别、性别和法律职业资格 A 证进行判断；
 * 其余疑似硬性限制返回 UNCERTAIN，避免把无法解析的资格条件误判为可报。
 */
@Component
public class RemarkMatcher implements JobConditionMatcher {

    private static final Pattern GRADUATE_YEAR = Pattern.compile("限?(\\d{4})届(?:普通高校|普通高等学校)?毕业生");
    private static final Pattern LEGAL_A_CERTIFICATE = Pattern.compile("法律职业资格(?:证书)?A证|法律职业资格考试.*成绩合格");
    private static final Pattern MILITARY_SERVICE_YEARS = Pattern.compile("(?:军队|部队)?服役\\s*(\\d+)\\s*年");
    private static final Pattern CET_REQUIREMENT = Pattern.compile("(?:全国)?大学英语([四六46])级(?:考试)?(?:成绩)?(?:达到|不低于|不少于)?(\\d{3})?分?(?:及以上|以上)?");

    @Override
    public ConditionType support() {
        return ConditionType.REMARK;
    }

    @Override
    public MatchItemResult match(UserProfile profile, JobPosition position, MatchContext context) {
        String remarkRaw = position.getRemark();
        String remark = TextNormalizer.normalize(remarkRaw);
        if (remark.isEmpty()) {
            return build(MatchResult.MATCH, null, remarkRaw, "岗位未设置备注条件，无需额外核验。");
        }

        List<String> userValues = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        MatchResult result = MatchResult.MATCH;
        boolean recognized = false;

        Matcher graduateMatcher = GRADUATE_YEAR.matcher(remark);
        if (graduateMatcher.find()) {
            recognized = true;
            int requiredYear = Integer.parseInt(graduateMatcher.group(1));
            MatchResult graduateResult = matchGraduateYear(profile, requiredYear, userValues, reasons);
            result = worse(result, graduateResult);
        }

        MatchResult genderResult = matchGender(profile, remark, userValues, reasons);
        if (genderResult != null) {
            recognized = true;
            result = worse(result, genderResult);
        }

        if (LEGAL_A_CERTIFICATE.matcher(remark).find()) {
            recognized = true;
            MatchResult certificateResult = matchLegalCertificate(profile, userValues, reasons);
            result = worse(result, certificateResult);
        }

        MatchResult cetResult = matchCet(profile, remark, userValues, reasons);
        if (cetResult != null) {
            recognized = true;
            result = worse(result, cetResult);
        }

        if (containsServiceProjectOrVeteranRestriction(remark)) {
            recognized = true;
            MatchResult serviceResult = matchServiceProjectOrVeteran(profile, remark, userValues, reasons);
            result = worse(result, serviceResult);
        }

        if (!recognized && looksLikeQualificationRestriction(remark)) {
            result = MatchResult.UNCERTAIN;
            reasons.add("备注包含疑似报考限制，当前档案字段无法可靠判断，请人工核验。");
        } else if (!recognized) {
            reasons.add("备注未识别到需要与个人档案核验的报考限制。请同时阅读岗位说明。");
        }

        return build(result, join(userValues), remarkRaw, join(reasons));
    }

    private MatchResult matchCet(UserProfile profile, String remark, List<String> userValues, List<String> reasons) {
        Matcher matcher = CET_REQUIREMENT.matcher(remark);
        MatchResult result = null;
        while (matcher.find()) {
            int requiredLevel = "六".equals(matcher.group(1)) || "6".equals(matcher.group(1)) ? 6 : 4;
            Integer requiredScore = matcher.group(2) == null ? null : Integer.valueOf(matcher.group(2));
            String level = TextNormalizer.normalize(profile.getEnglishLevel()).toUpperCase(java.util.Locale.ROOT);
            userValues.add("英语等级：" + englishLevelDisplay(level));
            if (level.isEmpty()) {
                reasons.add("备注要求大学英语" + requiredLevel + "级，但档案尚未选择英语等级，无法确认。 ");
                result = worse(result == null ? MatchResult.MATCH : result, MatchResult.UNCERTAIN);
                continue;
            }
            int actualLevel = "CET6".equals(level) ? 6 : "CET4".equals(level) ? 4 : 0;
            if (actualLevel < requiredLevel) {
                reasons.add("档案英语等级为“" + englishLevelDisplay(level) + "”，不符合大学英语" + requiredLevel + "级要求。");
                result = worse(result == null ? MatchResult.MATCH : result, MatchResult.NOT_MATCH);
                continue;
            }
            if (requiredScore != null && requiredScore > 425) {
                reasons.add("档案仅记录英语等级通过，无法确认是否达到" + requiredScore + "分。");
                result = worse(result == null ? MatchResult.MATCH : result, MatchResult.UNCERTAIN);
                continue;
            }
            reasons.add("档案已通过大学英语" + actualLevel + "级；通过成绩不低于425分，符合岗位要求。 ");
            result = worse(result == null ? MatchResult.MATCH : result, MatchResult.MATCH);
        }
        return result;
    }

    private String englishLevelDisplay(String level) {
        if ("CET6".equals(level)) return "已通过六级";
        if ("CET4".equals(level)) return "已通过四级";
        if ("NONE".equals(level)) return "未通过四级";
        return "未填写";
    }

    private MatchResult matchGraduateYear(UserProfile profile, int requiredYear,
                                          List<String> userValues, List<String> reasons) {
        String freshStatus = TextNormalizer.normalize(profile.getFreshGraduateStatus());
        LocalDate graduationDate = profile.getGraduationDate();
        userValues.add("应届身份：" + display(profile.getFreshGraduateStatus()));
        if (graduationDate != null) {
            userValues.add("毕业日期：" + graduationDate);
            if (graduationDate.getYear() == requiredYear) {
                reasons.add("毕业年份为" + requiredYear + "年，符合备注中的“限" + requiredYear + "届毕业生”要求。");
                return MatchResult.MATCH;
            }
            reasons.add("毕业年份为" + graduationDate.getYear() + "年，不符合备注中的“限" + requiredYear + "届毕业生”要求。");
            return MatchResult.NOT_MATCH;
        }
        if (isNo(freshStatus)) {
            reasons.add("档案中的应届身份为“否”，不符合备注中的“限" + requiredYear + "届毕业生”要求。");
            return MatchResult.NOT_MATCH;
        }
        reasons.add("备注要求限" + requiredYear + "届毕业生，但档案未填写毕业日期，无法确认具体届别。");
        return MatchResult.UNCERTAIN;
    }

    private MatchResult matchGender(UserProfile profile, String remark,
                                    List<String> userValues, List<String> reasons) {
        String required = null;
        if (remark.contains("限男性")) {
            required = "男";
        } else if (remark.contains("限女性")) {
            required = "女";
        }
        if (required == null) {
            return null;
        }
        String gender = normalizeGender(profile.getGender());
        userValues.add("性别：" + display(profile.getGender()));
        if (gender == null) {
            reasons.add("备注限" + ("男".equals(required) ? "男性" : "女性") + "，但档案未填写或无法识别性别。");
            return MatchResult.UNCERTAIN;
        }
        if (required.equals(gender)) {
            reasons.add("档案性别符合备注中的性别限制。");
            return MatchResult.MATCH;
        }
        reasons.add("档案性别不符合备注中的性别限制。");
        return MatchResult.NOT_MATCH;
    }

    private MatchResult matchLegalCertificate(UserProfile profile,
                                               List<String> userValues, List<String> reasons) {
        String certificates = TextNormalizer.normalize(profile.getCertificates());
        userValues.add("资格证书：" + display(profile.getCertificates()));
        if (certificates.isEmpty()) {
            reasons.add("备注要求法律职业资格A证或法考成绩合格，但档案未填写证书信息。");
            return MatchResult.UNCERTAIN;
        }
        String compact = certificates.replace(" ", "");
        if ((compact.contains("法律职业资格") || compact.contains("法考"))
                && (compact.contains("A证") || compact.contains("A级") || compact.contains("A类") || compact.contains("成绩合格"))) {
            reasons.add("档案中的资格证书信息符合备注中的法律职业资格A证要求。");
            return MatchResult.MATCH;
        }
        reasons.add("档案中的资格证书信息未包含法律职业资格A证或法考成绩合格信息。");
        return MatchResult.NOT_MATCH;
    }

    private MatchResult matchServiceProjectOrVeteran(UserProfile profile, String remark,
                                                      List<String> userValues, List<String> reasons) {
        String veteran = TextNormalizer.normalize(profile.getVeteran());
        String serviceProject = TextNormalizer.normalize(profile.getServiceProjectType());
        String notes = TextNormalizer.normalize(profile.getNotes());
        userValues.add("退役军人：" + display(profile.getVeteran()));
        userValues.add("服务基层项目：" + display(profile.getServiceProjectType()));
        if (!notes.isEmpty()) {
            userValues.add("档案备注：" + profile.getNotes().trim());
        }

        MatchResult veteranResult = matchVeteranAlternative(profile, veteran, notes, remark, reasons);
        MatchResult serviceResult = matchServiceProjectAlternative(serviceProject, notes, reasons);

        // 备注使用“或”：任一分支满足即可；两个分支均明确不满足才判不符合。
        if (veteranResult == MatchResult.MATCH || serviceResult == MatchResult.MATCH) {
            return MatchResult.MATCH;
        }
        if (veteranResult == MatchResult.UNCERTAIN || serviceResult == MatchResult.UNCERTAIN) {
            return MatchResult.UNCERTAIN;
        }
        return MatchResult.NOT_MATCH;
    }

    private MatchResult matchVeteranAlternative(UserProfile profile, String veteran, String notes,
                                                 String remark, List<String> reasons) {
        if (isNo(veteran)) {
            reasons.add("档案显示非退役军人，不满足“大学生退役士兵”分支。");
            return MatchResult.NOT_MATCH;
        }
        if (!isYes(veteran)) {
            reasons.add("档案未明确是否为退役军人，无法核验退役士兵分支。");
            return MatchResult.UNCERTAIN;
        }

        if (!isCollegeEducation(profile.getEducation()) && !notes.contains("大学生退役士兵")) {
            if (TextNormalizer.isBlank(profile.getEducation())) {
                reasons.add("档案已填写退役军人，但未填写学历，无法确认是否属于大学生退役士兵。");
                return MatchResult.UNCERTAIN;
            }
            reasons.add("档案学历不满足“大学生退役士兵”条件。");
            return MatchResult.NOT_MATCH;
        }

        int requiredYears = militaryYears(remark);
        int actualYears = militaryYears(notes);
        if (requiredYears > 0 && actualYears < 0) {
            reasons.add("档案符合大学生退役士兵身份，但未填写军队服役年限，无法确认是否满" + requiredYears + "年。");
            return MatchResult.UNCERTAIN;
        }
        if (requiredYears > 0 && actualYears < requiredYears) {
            reasons.add("档案备注显示军队服役" + actualYears + "年，未达到备注要求的" + requiredYears + "年。");
            return MatchResult.NOT_MATCH;
        }
        reasons.add(requiredYears > 0
                ? "档案符合大学生退役士兵身份，且军队服役年限达到" + requiredYears + "年要求。"
                : "档案符合大学生退役士兵身份要求。");
        return MatchResult.MATCH;
    }

    private MatchResult matchServiceProjectAlternative(String serviceProject, String notes,
                                                        List<String> reasons) {
        if (serviceProject.isEmpty() || isNo(serviceProject)) {
            reasons.add("档案未填写基层项目经历，不满足基层项目服务期满人员分支。");
            return MatchResult.NOT_MATCH;
        }
        if (!isAllowedServiceProject(serviceProject)) {
            reasons.add("档案中的基层项目不属于大学生村官、三支一扶、西部志愿者或农村特岗教师范围。");
            return MatchResult.NOT_MATCH;
        }
        String completionText = serviceProject + " " + notes;
        if (!completionText.contains("期满") && !completionText.contains("服务完成")) {
            reasons.add("档案中的基层项目类型符合，但未填写“服务期满”，需人工确认服务状态。");
            return MatchResult.UNCERTAIN;
        }
        reasons.add("档案中的基层项目类型符合，且已注明服务期满。");
        return MatchResult.MATCH;
    }

    private boolean isAllowedServiceProject(String value) {
        return value.contains("大学生村官") || value.contains("三支一扶")
                || value.contains("西部志愿") || value.contains("西部计划")
                || value.contains("农村特岗") || value.contains("特岗教师");
    }

    private boolean isCollegeEducation(String value) {
        String education = TextNormalizer.normalize(value);
        return education.contains("专科") || education.contains("大专") || education.contains("本科")
                || education.contains("硕士") || education.contains("研究生") || education.contains("博士");
    }

    private int militaryYears(String value) {
        Matcher matcher = MILITARY_SERVICE_YEARS.matcher(TextNormalizer.normalize(value));
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    private boolean containsServiceProjectOrVeteranRestriction(String remark) {
        return (remark.contains("退役士兵") || remark.contains("退役军人"))
                && (remark.contains("基层项目") || remark.contains("三支一扶") || remark.contains("大学生村官"));
    }

    private boolean looksLikeQualificationRestriction(String remark) {
        return remark.contains("限") || remark.contains("须") || remark.contains("要求")
                || remark.contains("取得") || remark.contains("具备") || remark.contains("报考");
    }

    private MatchResult worse(MatchResult left, MatchResult right) {
        if (left == MatchResult.NOT_MATCH || right == MatchResult.NOT_MATCH) return MatchResult.NOT_MATCH;
        if (left == MatchResult.UNCERTAIN || right == MatchResult.UNCERTAIN) return MatchResult.UNCERTAIN;
        return MatchResult.MATCH;
    }

    private String normalizeGender(String value) {
        String normalized = TextNormalizer.normalize(value);
        if ("男".equals(normalized) || "男性".equals(normalized)) return "男";
        if ("女".equals(normalized) || "女性".equals(normalized)) return "女";
        return null;
    }

    private boolean isYes(String value) {
        return "是".equals(value) || "有".equals(value) || "退役军人".equals(value) || "退役士兵".equals(value);
    }

    private boolean isNo(String value) {
        return "否".equals(value) || "无".equals(value) || "不是".equals(value);
    }

    private String display(String value) {
        return TextNormalizer.isBlank(value) ? "未填写" : value.trim();
    }

    private String join(List<String> values) {
        return values.isEmpty() ? null : String.join("；", values);
    }

    private MatchItemResult build(MatchResult result, String userValue, String requirementValue, String reason) {
        return new MatchItemResult(ConditionType.REMARK, result, userValue, requirementValue, reason);
    }
}
