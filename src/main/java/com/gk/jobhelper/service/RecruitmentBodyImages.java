package com.gk.jobhelper.service;

import java.net.URI;
import java.net.URLEncoder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

/** 公告正文图片地址规范化与受控代理展示。 */
public final class RecruitmentBodyImages {
    private RecruitmentBodyImages() { }

    public static void normalize(Element body, String noticeUrl) {
        for (Element image : body.select("img")) {
            String resolved = "";
            for (String attr : new String[]{"data-src", "data-original", "data-url", "src"}) {
                try {
                    String value = image.attr(attr).trim();
                    if (value.isEmpty()) continue;
                    URI uri = URI.create(noticeUrl).resolve(value).normalize();
                    if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) {
                        resolved = uri.toString();
                        break;
                    }
                } catch (IllegalArgumentException ignored) { }
            }
            image.removeAttr("srcset").removeAttr("data-src").removeAttr("data-original").removeAttr("data-url");
            image.attr("src", resolved);
        }
        body.select("source").remove();
    }

    public static String proxy(String html, String noticeUrl, Long id) {
        Element body = Jsoup.parseBodyFragment(html == null ? "" : html, noticeUrl).body();
        normalize(body, noticeUrl);
        for (Element image : body.select("img")) {
            try {
                String source = image.attr("src");
                image.attr("src", "/api/recruitment/notices/" + id + "/image?url=" + URLEncoder.encode(source, "UTF-8"));
            } catch (java.io.UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }
        return body.html();
    }
}
