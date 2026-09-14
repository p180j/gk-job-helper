package com.gk.jobhelper.service;

import com.gk.jobhelper.dto.RecruitmentNoticeCandidate;
import com.gk.jobhelper.entity.RecruitmentSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/** Conservative generic list-page reader for user-configured public recruitment sites. */
@Component
public class GenericRecruitmentSource {
    private static final Pattern DATE = Pattern.compile("(20\\d{2}[-/.]\\d{1,2}[-/.]\\d{1,2})");
    private static final Pattern RECRUITMENT = Pattern.compile("招聘|招募|招录|诚聘|人才引进|校园招聘|社会招聘");

    public List<RecruitmentNoticeCandidate> fetch(RecruitmentSource source) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(source.getListUrl()).openConnection();
            connection.setConnectTimeout(10000); connection.setReadTimeout(20000); connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (connection.getResponseCode() != 200) throw new IllegalStateException("招聘网站返回 HTTP " + connection.getResponseCode());
            Document document = Jsoup.parse(connection.getInputStream(), null, source.getListUrl());
            List<RecruitmentNoticeCandidate> result = parse(document, source.getListUrl());
            if (result.isEmpty()) throw new IllegalStateException("列表页未识别到招聘公告链接，请确认填写的是招聘公告列表地址");
            return result;
        } catch (IllegalStateException e) { throw e;
        } catch (Exception e) { throw new IllegalStateException("访问招聘公告列表失败：" + safe(e.getMessage()), e); }
    }

    private List<RecruitmentNoticeCandidate> parse(Document document, String baseUrl) {
        List<RecruitmentNoticeCandidate> result = new ArrayList<>(); Set<String> urls = new LinkedHashSet<>();
        for (Element link : document.select("a[href]")) {
            String title = link.text().trim(), href = link.attr("href").trim();
            if (title.length() < 4 || href.isEmpty() || href.startsWith("#") || href.startsWith("javascript:") || !RECRUITMENT.matcher(title).find()) continue;
            String url;
            try { url = URI.create(baseUrl).resolve(href).normalize().toString(); } catch (Exception ignored) { continue; }
            if (!urls.add(url)) continue;
            result.add(new RecruitmentNoticeCandidate(title, url, date(link.parent() == null ? "" : link.parent().text())));
        }
        return result;
    }

    private LocalDateTime date(String text) { Matcher matcher = DATE.matcher(text); if (!matcher.find()) return null; try { return LocalDate.parse(matcher.group(1).replace('.', '-').replace('/', '-'), DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(); } catch (Exception ignored) { return null; } }
    private String safe(String value) { return value == null || value.trim().isEmpty() ? "网络连接失败" : value; }
}
