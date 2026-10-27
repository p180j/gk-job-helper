package com.gk.jobhelper.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Offline 2025 administrative divisions. Names shared by several provinces remain ambiguous. */
@Component
public class AdministrativeDivisionCatalog {
    private final Map<String, List<Division>> byName = new HashMap<>();
    private final List<Division> all = new ArrayList<>();

    public AdministrativeDivisionCatalog(ObjectMapper objectMapper) throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/administrative-divisions-2025.json")) {
            if (input == null) throw new IOException("行政区划数据缺失");
            JsonNode tree = objectMapper.readTree(input);
            load(tree, "0", null, null);
        }
        all.sort(Comparator.comparingInt((Division d) -> d.name.length()).reversed()
                .thenComparing(Comparator.comparingInt((Division d) -> d.code.length()).reversed()));
    }

    private void load(JsonNode tree, String parentCode, String province, String city) {
        JsonNode children = tree.path(parentCode);
        if (!children.isArray()) return;
        for (JsonNode child : children) {
            String code = child.path("c").asText();
            String name = child.path("n").asText();
            String provinceName = province == null ? name : province;
            String cityName = province == null ? null : city == null ? name : city;
            if ("市辖区".equals(name)) cityName = provinceName;
            Division division = new Division(code, name, provinceName, cityName);
            if (!"市辖区".equals(name)) {
                all.add(division);
                byName.computeIfAbsent(name, k -> new ArrayList<>()).add(division);
            }
            load(tree, code, provinceName, cityName);
        }
    }

    public Division findInText(String text) {
        if (text == null) return null;
        for (Division candidate : all) {
            if (!text.contains(candidate.name)) continue;
            List<Division> namesakes = byName.getOrDefault(candidate.name, Collections.emptyList());
            if (namesakes.size() == 1) return candidate;
            for (Division namesake : namesakes) {
                if (text.contains(namesake.province) || namesake.city != null && text.contains(namesake.city)) {
                    return namesake;
                }
            }
        }
        return null;
    }

    public static class Division {
        private final String code, name, province, city;
        Division(String code, String name, String province, String city) {
            this.code = code; this.name = name; this.province = province; this.city = city;
        }
        public String getCode() { return code; }
        public String getName() { return name; }
        public String getProvince() { return province; }
        public String getCity() { return city; }
        public String getDisplayName() {
            if (name.equals(province)) return province;
            if (city == null || name.equals(city)) return province + " " + name;
            return province + " " + city + " " + name;
        }
    }
}
