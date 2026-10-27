package com.gk.jobhelper.matcher;

import com.gk.jobhelper.common.TextNormalizer;
import com.gk.jobhelper.constant.ConditionType;
import com.gk.jobhelper.constant.MatchResult;
import com.gk.jobhelper.entity.JobPosition;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.service.JobRemarkParser;
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
    private final JobRemarkParser remarkParser = new JobRemarkParser();

    private static final Pattern GRADUATE_YEAR = Pattern.compile("(?:限|仅限)?(\\d{4})届(?:(?:普通)?高校|普通高等学校)?毕业生");
    private static final Pattern GENDER = Pattern.compile("(?:限|仅限)?(?:男性|女性)");
    private static final Pattern LEGAL_A_CERTIFICATE = Pattern.compile("法律职业资格(?:证书)?\\s*\\(?A证\\)?|法律职业资格考试[^;。]*成绩合格");
    private static final Pattern MILITARY_SERVICE_YEARS = Pattern.compile("(?:军队|部队)?服役\\s*(\\d+)\\s*年");
    private static final Pattern CET_REQUIREMENT = Pattern.compile("(?:全国)?大学英语([四六46])级(?:考试)?(?:成绩)?(?:达到|不低于|不少于|在|为)?(\\d{3})?分?(?:及以上|以上)?");

    @Override
    public ConditionType support() {
        return ConditionType.REMARK;
    }

    @Override
    public MatchItemResult match(UserProfile profile, JobPosition position, MatchContext context) {
        String remarkRaw = joinRequirements(position.getAdditionalRequirements(), position.getRemark());
        String remark = TextNormalizer.normalize(remarkRaw);
        if (remark.isEmpty()) {
            return build(MatchResult.MATCH, null, remarkRaw, "岗位未设置其他条件或备注限制，无需额外核验。");
        }
        for (String clause : JobRemarkParser.splitClauses(remarkRaw)) {
            if (unresolvedAlternative(clause)) {
                return build(MatchResult.UNCERTAIN, null, remarkRaw,
                        "其他条件或备注包含尚不能完整核验的“或”分支，需核实各备选条件，不能按单一分支排除或放行。");
            }
        }

        List<String> userValues = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        MatchResult result = MatchResult.MATCH;
        boolean recognized = false;

        for (JobRemarkParser.Condition condition : remarkParser.parse(position)) {
            if ("MIN_SERVICE_YEARS".equals(condition.getType())) {
                recognized = true;
                reasons.add("最低服务" + condition.getYears() + "年，录用后需履行相关服务约束，请确认是否接受。");
            } else if ("WORK_LOCATION".equals(condition.getType())) {
                recognized = true;
                reasons.add("备注说明" + condition.getSourceText() + "，请核对岗位地点。");
            } else if ("CERTIFICATE".equals(condition.getType())
                    && !condition.getMatchedText().contains("法律职业资格")) {
                recognized = true;
                result = worse(result, MatchResult.UNCERTAIN);
                reasons.add("备注要求" + condition.getSourceText() + "，档案当前无法可靠核验该证书。");
            } else if ("POLICE_PHYSICAL".equals(condition.getType())
                    || "POLITICAL_IDENTITY".equals(condition.getType())
                    || "DISQUALIFICATION".equals(condition.getType())
                    || "UNKNOWN".equals(condition.getType())) {
                recognized = true;
                result = worse(result, MatchResult.UNCERTAIN);
                reasons.add("备注条件“" + condition.getSourceText() + "”尚无法据当前档案可靠核验。");
            }
        }

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
            Boolean applies = legalCertificateApplies(profile, remark);
            if (Boolean.FALSE.equals(applies)) {
                reasons.add("法律职业资格A证条件仅适用于备注指定专业，当前档案专业不在该明确范围内。");
            } else if (applies == null) {
                result = worse(result, MatchResult.UNCERTAIN);
                reasons.add("法律职业资格A证条件有专业适用范围，当前无法可靠确认是否适用。");
            } else {
                MatchResult certificateResult = matchLegalCertificate(profile, userValues, reasons);
                result = worse(result, certificateResult);
            }
        }

        MatchResult cetResult = matchCet(profile, remark, userValues, reasons);
        if (cetResult != null) {
            recognized = true;
            result = worse(result, cetResult);
        }

        if (containsServiceProjectRestriction(remark)) {
            recognized = true;
            MatchResult serviceResult = containsVeteranRestriction(remark) && remark.contains("或")
                    ? matchServiceProjectOrVeteran(profile, remark, userValues, reasons)
                    : matchServiceProject(profile, remark, userValues, reasons);
            result = worse(result, serviceResult);
        }

        if (hasUnresolvedQualificationClause(remark, context)) {
            result = worse(result, MatchResult.UNCERTAIN);
            reasons.add("备注仍包含未识别的疑似报考限制，请人工核验对应原文。");
        } else if (!recognized) {
            reasons.add("备注未识别到需要与个人档案核验的报考限制。请同时阅读岗位说明。");
        }

        return build(result, join(userValues), remarkRaw, join(reasons));
    }

    public List<MatchItemResult> detailMatches(UserProfile profile, JobPosition position, MatchContext context,
                                                List<MatchItemResult> educationItems) {
        List<MatchItemResult> details = new ArrayList<>();
        for (JobRemarkParser.Condition condition : remarkParser.parse(position)) {
            List<String> values = new ArrayList<>();
            List<String> reasons = new ArrayList<>();
            MatchResult verdict = detailVerdict(profile, position, context, educationItems, condition, values, reasons);
            MatchEvidence evidence = new MatchEvidence();
            evidence.setRuleId(condition.getRuleId());
            evidence.setSourceField(condition.getSourceField());
            evidence.setParseStatus(condition.getStatus());
            evidence.setParserVersion(condition.getParserVersion());
            evidence.setManualReview(verdict == MatchResult.UNCERTAIN || "UNKNOWN".equals(condition.getStatus()));
            if (verdict == MatchResult.UNCERTAIN) evidence.setMissingFields(missingFields(condition));
            details.add(new MatchItemResult(ConditionType.REMARK_CONDITION, verdict, join(values),
                    condition.getSourceText(), join(reasons), evidence));
        }
        return details;
    }

    private MatchResult detailVerdict(UserProfile profile, JobPosition position, MatchContext context,
                                      List<MatchItemResult> educationItems, JobRemarkParser.Condition condition,
                                      List<String> values, List<String> reasons) {
        String type = condition.getType();
        String source = condition.getSourceText();
        if (!"QUALIFICATION".equals(condition.getNature())) {
            reasons.add("该条属于岗位说明或录用后约束，请阅读原文。");
            return MatchResult.MATCH;
        }
        if (unresolvedAlternative(source)) return uncertain(reasons, "该条包含未完整支持的备选分支，需人工核验各分支。");
        if ("WORK_EXPERIENCE".equals(type)) {
            MatchItemResult item = new WorkExperienceMatcher().matchRemarkCondition(profile, position, context, source);
            if (item == null) return uncertain(reasons, "工作经历表达式未被完整识别，需核验原文。");
            if (item.getUserValue() != null) values.add(item.getUserValue());
            reasons.add(item.getReason());
            return item.getResult();
        }
        if ("MAJOR_SCOPE".equals(type) || "EDUCATION_SCOPE".equals(type)) {
            MatchItemResult item = educationItems.get(1);
            if (item.getUserValue() != null) values.add(item.getUserValue());
            reasons.add(item.getReason());
            return item.getResult();
        }
        if ("GRADUATE_STATUS".equals(type)) {
            if (source.contains("非应届毕业生应为")) {
                String status = TextNormalizer.normalize(profile.getFreshGraduateStatus());
                if (status.isEmpty()) return uncertain(reasons, "档案缺少应届身份，无法确定工作经历限制是否适用。");
                reasons.add("该条限定非应届毕业生的适用范围，具体资格由工作经历子条件判断。");
                return MatchResult.MATCH;
            }
            Matcher year = GRADUATE_YEAR.matcher(source);
            if (year.find()) return matchGraduateYear(profile, Integer.parseInt(year.group(1)), values, reasons);
            String status = TextNormalizer.normalize(profile.getFreshGraduateStatus());
            if (status.isEmpty()) return uncertain(reasons, "档案未填写应届身份。");
            boolean requiredFresh = !source.contains("非应届");
            boolean actualFresh = isYes(status);
            reasons.add("按档案应届身份与原文限制核验。");
            return requiredFresh == actualFresh ? MatchResult.MATCH : MatchResult.NOT_MATCH;
        }
        if ("GENDER".equals(type)) {
            MatchResult result = matchGender(profile, source, values, reasons);
            return result == null ? uncertain(reasons, "性别要求无法可靠识别。") : result;
        }
        if ("LANGUAGE_EXAM".equals(type)) {
            MatchResult result = matchCet(profile, source, values, reasons);
            return result == null ? uncertain(reasons, "档案尚不能核验该外语考试或成绩要求。") : result;
        }
        if ("CERTIFICATE".equals(type) && condition.getMatchedText().contains("法律职业资格")) {
            Boolean applies = legalCertificateApplies(profile, joinRequirements(position.getAdditionalRequirements(), position.getRemark()));
            if (Boolean.FALSE.equals(applies)) {
                reasons.add("该证书仅对指定专业适用，当前专业不在该范围。");
                return MatchResult.MATCH;
            }
            if (applies == null) return uncertain(reasons, "无法确认该证书条件是否适用于档案专业。");
            return matchLegalCertificate(profile, values, reasons);
        }
        if ("DIRECTED_IDENTITY".equals(type) && containsServiceProjectRestriction(source)) {
            String combined = joinRequirements(position.getAdditionalRequirements(), position.getRemark());
            if (containsVeteranRestriction(source) && source.contains("或")) {
                return matchServiceProjectOrVeteran(profile, combined, values, reasons);
            }
            return matchServiceProject(profile, combined, values, reasons);
        }
        return uncertain(reasons, "该资格条件仍需结合原文和个人证明材料核验。");
    }

    private MatchResult uncertain(List<String> reasons, String reason) {
        reasons.add(reason);
        return MatchResult.UNCERTAIN;
    }

    private boolean unresolvedAlternative(String text) {
        if (!text.contains("或") || !looksLikeQualificationRestriction(text)
                && !GENDER.matcher(text).find() && !CET_REQUIREMENT.matcher(text).find()) return false;
        if (containsServiceProjectRestriction(text)
                && !text.matches(".*(?:资格证|工作经历|在职|单位性质|学历|英语).*")) return false;
        if (text.matches(".*专业(?:须|需|应|必须)(?:取得|具有|持有)法律职业资格.*")) return false;
        if ((text.contains("法考成绩合格") || text.contains("法律职业资格考试"))
                && !text.matches(".*(?:教师|医师|会计|建造师)资格.*")) return false;
        return !text.matches(".*(?:雅思|托福|英语专业[四八]级).*")
                || text.matches(".*(?:工作经历|单位性质|教师资格|法律职业资格).*");
    }

    private String missingFields(JobRemarkParser.Condition condition) {
        if ("WORK_EXPERIENCE".equals(condition.getType())) return "分段工作时间、地区、单位性质、行业、岗位、在职及核验状态";
        if ("GRADUATE_STATUS".equals(condition.getType())) return "毕业日期或应届身份";
        if ("CERTIFICATE".equals(condition.getType())) return "证书名称和取得状态";
        if ("MAJOR_SCOPE".equals(condition.getType()) || "EDUCATION_SCOPE".equals(condition.getType())) {
            return "各学历阶段、主修专业及学位信息";
        }
        return "原文所要求的对应证明材料";
    }

    private String joinRequirements(String additional, String remark) {
        if (TextNormalizer.isBlank(additional)) return remark;
        if (TextNormalizer.isBlank(remark)) return additional;
        return additional.trim() + "；" + remark.trim();
    }

    private Boolean legalCertificateApplies(UserProfile profile, String text) {
        text = TextNormalizer.normalize(text);
        Matcher condition = Pattern.compile("(?:^|[;。])([^;。]{1,60}?)专业(?:须|需|应|必须)(?:取得|具有|持有)法律职业资格").matcher(text);
        if (!condition.find()) return text.matches(".*专业[^;。]{0,40}法律职业资格.*") ? null : true;
        String major = TextNormalizer.normalize(profile.getMajor()).replace("专业", "");
        if (major.isEmpty()) return null;
        String[] alternatives = condition.group(1).replace("仅限", "").replace("限", "").split("[、,，或]");
        for (String alternative : alternatives) {
            String item = alternative.trim().replace("专业", "");
            if (item.equals(major)) return true;
            if (item.contains("类") || item.contains("相关") || item.contains("学科")) return null;
        }
        return false;
    }

    private boolean hasUnresolvedQualificationClause(String remark, MatchContext context) {
        for (String clause : remark.split("[;；。]+")) {
            String text = clause.trim();
            if (!looksLikeQualificationRestriction(text)) continue;
            // 工作经历的组合限定由 WorkExperienceMatcher 一次性核验；未知限定在那里返回 UNCERTAIN。
            if (!context.getWorkExperiences().isEmpty()
                    && text.contains("工作") && (text.contains("年") || text.contains("经历"))
                    && !text.matches(".*(?:证书|资格|党员|学历|专业|不得|体检).*")) continue;
            String residual = JobRemarkParser.removeRecognizedInformationalText(text);
            if (!looksLikeQualificationRestriction(residual)) continue;
            if (GRADUATE_YEAR.matcher(residual).find()
                    || residual.matches(".*(?:限|仅限)?(?:男性|女性)(?:[，,；;。.]|$).*")) {
                String remaining = GRADUATE_YEAR.matcher(residual).replaceAll("")
                        .replaceAll("(?:仅限|限)?(?:男性|女性)", "")
                        .replaceAll("^[，,\\s]*报考[，,\\s]*$", "");
                if (looksLikeQualificationRestriction(remaining)) return true;
                continue;
            }
            if (LEGAL_A_CERTIFICATE.matcher(residual).find() || CET_REQUIREMENT.matcher(residual).find()
                    || containsServiceProjectRestriction(residual)) continue;
            return true;
        }
        return false;
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
        if (remark.matches(".*(?:雅思|托福|英语专业[四八]级).*")) {
            boolean alternatives = remark.contains("或") || remark.contains("亦可") || remark.contains("之一");
            if (!alternatives || result != MatchResult.MATCH) {
                reasons.add("原文还包含其他外语考试或成绩分支，档案未记录这些考试成绩，需核验。 ");
                return MatchResult.UNCERTAIN;
            }
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
        if (remark.matches(".*(?:限|仅限)?男性(?:[，,；;。.]|$).*")) {
            required = "男";
        } else if (remark.matches(".*(?:限|仅限)?女性(?:[，,；;。.]|$).*")) {
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
        MatchResult serviceResult = matchServiceProjectAlternative(serviceProject, notes, remark, reasons);

        // 备注使用“或”：任一分支满足即可；两个分支均明确不满足才判不符合。
        if (veteranResult == MatchResult.MATCH || serviceResult == MatchResult.MATCH) {
            return MatchResult.MATCH;
        }
        if (veteranResult == MatchResult.UNCERTAIN || serviceResult == MatchResult.UNCERTAIN) {
            return MatchResult.UNCERTAIN;
        }
        return MatchResult.NOT_MATCH;
    }

    private MatchResult matchServiceProject(UserProfile profile, String remark,
                                             List<String> userValues, List<String> reasons) {
        String serviceProject = TextNormalizer.normalize(profile.getServiceProjectType());
        String notes = TextNormalizer.normalize(profile.getNotes());
        userValues.add("服务基层项目：" + display(profile.getServiceProjectType()));
        if (!notes.isEmpty()) {
            userValues.add("档案备注：" + profile.getNotes().trim());
        }
        return matchServiceProjectAlternative(serviceProject, notes, remark, reasons);
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

    private MatchResult matchServiceProjectAlternative(String serviceProject, String notes, String remark,
                                                        List<String> reasons) {
        if (serviceProject.isEmpty() || isNo(serviceProject)) {
            reasons.add("档案未填写基层项目经历，不满足基层项目服务期满人员分支。");
            return MatchResult.NOT_MATCH;
        }
        if (!isAllowedServiceProject(serviceProject)) {
            reasons.add("档案中的基层项目不属于大学生村官、三支一扶、西部志愿者或农村特岗教师范围。");
            return MatchResult.NOT_MATCH;
        }
        String projectName = serviceProjectName(serviceProject);
        if (!isServiceProjectAllowedByRemark(projectName, remark)) {
            reasons.add("岗位备注列出的基层项目范围未包含“" + projectName + "”。");
            return MatchResult.NOT_MATCH;
        }
        String completionText = serviceProject + " " + notes;
        if (!completionText.contains("期满") && !completionText.contains("服务完成")) {
            reasons.add("档案中的基层项目类型符合，但未填写“服务期满”，需人工确认服务状态。");
            return MatchResult.UNCERTAIN;
        }
        if (remark.contains("考核合格") && !completionText.contains("考核合格")) {
            reasons.add("档案中的基层项目已注明服务期满，但未填写“考核合格”，需人工确认考核结果。");
            return MatchResult.UNCERTAIN;
        }
        reasons.add("档案中的基层项目类型符合，且已注明服务期满。");
        return MatchResult.MATCH;
    }

    private boolean isServiceProjectAllowedByRemark(String projectName, String remark) {
        boolean namesSpecificProjects = remark.contains("大学生村官") || remark.contains("三支一扶")
                || remark.contains("西部志愿") || remark.contains("西部计划")
                || remark.contains("农村特岗") || remark.contains("特岗教师");
        if (!namesSpecificProjects) {
            return remark.contains("基层项目");
        }
        if ("大学生村官".equals(projectName)) return remark.contains("大学生村官");
        if ("三支一扶".equals(projectName)) return remark.contains("三支一扶");
        if ("西部计划志愿者".equals(projectName)) {
            return remark.contains("西部志愿") || remark.contains("西部计划");
        }
        return remark.contains("农村特岗") || remark.contains("特岗教师");
    }

    private String serviceProjectName(String value) {
        if (value.contains("大学生村官")) return "大学生村官";
        if (value.contains("三支一扶")) return "三支一扶";
        if (value.contains("西部志愿") || value.contains("西部计划")) return "西部计划志愿者";
        return "农村特岗教师";
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

    private boolean containsServiceProjectRestriction(String remark) {
        boolean mentionsProject = remark.contains("基层项目") || remark.contains("三支一扶")
                || remark.contains("大学生村官") || remark.contains("西部志愿")
                || remark.contains("西部计划") || remark.contains("农村特岗") || remark.contains("特岗教师");
        return mentionsProject && (remark.contains("限") || remark.contains("定向招录")
                || remark.contains("服务期满") || remark.contains("考核合格") || remark.contains("报考"));
    }

    private boolean containsVeteranRestriction(String remark) {
        return remark.contains("退役士兵") || remark.contains("退役军人");
    }

    private boolean looksLikeQualificationRestriction(String remark) {
        return remark.contains("限") || remark.contains("须") || remark.contains("要求")
                || remark.contains("取得") || remark.contains("具备") || remark.contains("报考")
                || remark.contains("具有") || remark.contains("应为") || remark.contains("不得") || remark.contains("需");
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
