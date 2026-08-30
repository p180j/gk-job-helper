package com.gk.jobhelper.service;

import com.gk.jobhelper.dto.RecommendationCandidateRow;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Resolves a usable region without mutating imported job data. */
@Service
public class JobRegionResolver {
    private static final Map<String, String> JIANGXI_AREA_CODES = new LinkedHashMap<>();
    private static final String[] JIANGXI_CITIES = {
            "南昌市", "九江市", "上饶市", "抚州市", "宜春市", "吉安市",
            "赣州市", "景德镇市", "萍乡市", "新余市", "鹰潭市"
    };
    private static final String[] MUNICIPALITIES = {"北京市", "天津市", "上海市", "重庆市"};
    private static final Map<String, String> MUNICIPAL_DISTRICTS = new LinkedHashMap<>();
    private static final Pattern CITY_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5]{2,10}(?:自治州|地区|盟|市))");

    static {
        JIANGXI_AREA_CODES.put("0791", "江西省 南昌市");
        JIANGXI_AREA_CODES.put("0792", "江西省 九江市");
        JIANGXI_AREA_CODES.put("0793", "江西省 上饶市");
        JIANGXI_AREA_CODES.put("0794", "江西省 抚州市");
        JIANGXI_AREA_CODES.put("0795", "江西省 宜春市");
        JIANGXI_AREA_CODES.put("0796", "江西省 吉安市");
        JIANGXI_AREA_CODES.put("0797", "江西省 赣州市");
        JIANGXI_AREA_CODES.put("0798", "江西省 景德镇市");
        JIANGXI_AREA_CODES.put("0799", "江西省 萍乡市");
        JIANGXI_AREA_CODES.put("0701", "江西省 鹰潭市");
        JIANGXI_AREA_CODES.put("0790", "江西省 新余市");
        addDistricts("北京市", "东城区、西城区、朝阳区、丰台区、石景山区、海淀区、门头沟区、房山区、通州区、顺义区、昌平区、大兴区、怀柔区、平谷区、密云区、延庆区");
        addDistricts("天津市", "和平区、河东区、河西区、南开区、河北区、红桥区、东丽区、西青区、津南区、北辰区、武清区、宝坻区、滨海新区、宁河区、静海区、蓟州区");
        addDistricts("上海市", "黄浦区、徐汇区、长宁区、静安区、普陀区、虹口区、杨浦区、闵行区、宝山区、嘉定区、浦东新区、金山区、松江区、青浦区、奉贤区、崇明区");
        addDistricts("重庆市", "万州区、涪陵区、渝中区、大渡口区、江北区、沙坪坝区、九龙坡区、南岸区、北碚区、綦江区、大足区、渝北区、巴南区、黔江区、长寿区、江津区、合川区、永川区、南川区、璧山区、铜梁区、潼南区、荣昌区、开州区、梁平区、武隆区");
    }

    public String resolve(RecommendationCandidateRow row) {
        String explicit = row.getRegion();
        if (!blank(explicit)) return explicit;
        String evidence = evidence(row);
        for (String city : JIANGXI_CITIES) {
            if (evidence.contains(city) || evidence.contains(stripSuffix(city))) return "江西省 " + city;
        }
        for (Map.Entry<String, String> entry : JIANGXI_AREA_CODES.entrySet()) {
            if (evidence.contains(entry.getKey())) return entry.getValue();
        }
        for (Map.Entry<String, String> entry : MUNICIPAL_DISTRICTS.entrySet()) {
            if (evidence.contains(entry.getKey())) return entry.getValue() + " " + entry.getKey();
        }
        for (String municipality : MUNICIPALITIES) {
            if (evidence.contains(municipality) || evidence.contains(stripSuffix(municipality))) return municipality;
        }
        Matcher city = CITY_PATTERN.matcher(value(row.getDepartmentName()) + " " + value(row.getOrganizationName()));
        if (city.find()) return city.group(1);
        return "";
    }

    public boolean matches(List<String> preferredRegions, RecommendationCandidateRow row) {
        if (preferredRegions == null || preferredRegions.isEmpty()) return false;
        String resolved = resolve(row);
        String evidence = resolved + " " + evidence(row);
        for (String preference : preferredRegions) {
            if (blank(preference)) continue;
            String[] levels = preference.split("/");
            boolean allMatched = true;
            int start = levels.length > 1 ? levels.length - 1 : 0;
            for (int index = start; index < levels.length; index++) {
                String level = levels[index];
                String normalized = level.trim();
                if (normalized.isEmpty()) continue;
                String shortName = stripSuffix(normalized);
                if (!evidence.contains(normalized) && !evidence.contains(shortName)) {
                    allMatched = false;
                    break;
                }
            }
            if (allMatched) return true;
        }
        return false;
    }

    private String evidence(RecommendationCandidateRow row) {
        return value(row.getDepartmentName()) + " " + value(row.getOrganizationName()) + " " + value(row.getRawData());
    }

    private String stripSuffix(String value) {
        return value.replaceFirst("(特别行政区|自治区|自治州|省|市|区|县)$", "");
    }

    private String value(String value) { return value == null ? "" : value; }
    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private static void addDistricts(String municipality, String districts) {
        for (String district : districts.split("、")) MUNICIPAL_DISTRICTS.put(district, municipality);
    }
}
