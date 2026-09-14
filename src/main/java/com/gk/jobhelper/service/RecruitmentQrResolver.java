package com.gk.jobhelper.service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.gk.jobhelper.entity.RecruitmentAttachment;
import com.gk.jobhelper.mapper.RecruitmentAttachmentMapper;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

/** Resolves QR attachment hints into ordinary recruitment attachments without parsing positions itself. */
@Service
public class RecruitmentQrResolver {
    private static final int MAX_REDIRECTS = 3;
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final int MAX_RESPONSE_BYTES = 20 * 1024 * 1024;
    private static final Pattern MARKDOWN_LINK = Pattern.compile("\\[([^\\]]+)]\\((https?://[^)\\s]+)", Pattern.CASE_INSENSITIVE);
    private final RecruitmentAttachmentMapper attachments;
    private final RecruitmentAttachmentClassifier classifier;
    private final RecruitmentFileTypeDetector detector;
    private final RecruitmentDetailService details;

    public RecruitmentQrResolver(RecruitmentAttachmentMapper attachments, RecruitmentAttachmentClassifier classifier,
            RecruitmentFileTypeDetector detector, RecruitmentDetailService details) {
        this.attachments = attachments;
        this.classifier = classifier;
        this.detector = detector;
        this.details = details;
    }

    public Resolution resolve(Long noticeId) {
        Resolution result = new Resolution();
        for (RecruitmentAttachment hint : attachments.selectQrAttachmentHintsByNoticeId(noticeId)) {
            result.qrCount++;
            try {
                FetchContext context = new FetchContext();
                String target = decode(fetch(hint.getFileUrl(), MAX_IMAGE_BYTES, context).bytes);
                result.decodedCount++;
                Target resolved = target(target, context);
                if (resolved.kind == Kind.LIST_PAGE) result.listPageCount++;
                if (resolved.kind == Kind.CAOLIAO_UNRESOLVED) {
                    mark(hint, "CAOLIAO_ATTACHMENT_LIST_UNRESOLVED", "CAOLIAO_FILE_LIST_EXTRACT：已打开草料文件页，但未提取到可下载附件：" + resolved.sourceUrl);
                    result.caoliaoUnresolvedCount++;
                    result.warningCount++;
                    continue;
                }
                if (resolved.kind == Kind.NON_ATTACHMENT) {
                    mark(hint, "QR_MANUAL", "QR_NON_ATTACHMENT：二维码指向动态网页或报名页面，未提供可直接下载的附件：" + resolved.sourceUrl);
                    result.warningCount++;
                    continue;
                }
                if (resolved.attachments.isEmpty()) {
                    mark(hint, "QR_MANUAL", "QR_MANUAL：二维码未发现可下载附件");
                    result.warningCount++;
                    continue;
                }
                List<RecruitmentAttachmentDraft> drafts = new ArrayList<>();
                for (RecruitmentAttachmentDraft draft : resolved.attachments) {
                    String source = "QR_" + (resolved.kind == Kind.LIST_PAGE ? "ATTACHMENT_LIST" : "DIRECT_ATTACHMENT")
                            + "；二维码图片：" + hint.getFileUrl() + "；二维码目标：" + resolved.sourceUrl
                            + (draft.sourceText == null || draft.sourceText.isEmpty() ? "" : "；" + draft.sourceText);
                    drafts.add(new RecruitmentAttachmentDraft(draft.fileName, draft.fileUrl, draft.fileType, draft.attachmentType, source));
                    result.attachmentCount++;
                    if ("POSITION_DATA".equals(draft.attachmentType)) result.positionDataCount++;
                }
                details.saveAttachments(noticeId, drafts);
                mark(hint, "QR_RESOLVED", "二维码已解析，发现 " + drafts.size() + " 个附件");
            } catch (QrRestrictedException e) {
                mark(hint, "QR_MANUAL", "EXTERNAL_SOURCE_ACCESS_RESTRICTED：" + e.getMessage());
                result.warningCount++;
            } catch (CaoliaoException e) {
                mark(hint, e.stage, e.getMessage());
                result.caoliaoUnresolvedCount++;
                result.warningCount++;
            } catch (NotFoundException e) {
                mark(hint, "QR_DECODE_FAILED", "QR_DECODE_FAILED：未识别到二维码内容");
                result.warningCount++;
            } catch (Exception e) {
                mark(hint, "QR_MANUAL", "QR_MANUAL：" + safe(e.getMessage()));
                result.warningCount++;
            }
        }
        return result;
    }

    private String decode(byte[] bytes) throws Exception {
        ImageIO.scanForPlugins();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) throw new IllegalStateException("二维码图片格式无效");
        return new MultiFormatReader().decode(new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(image)))).getText();
    }

    private Target target(String value) throws Exception {
        return target(value, new FetchContext());
    }

    private Target target(String value, FetchContext context) throws Exception {
        Response response;
        try {
            response = fetch(value, MAX_RESPONSE_BYTES, context);
        } catch (Exception e) {
            if (caoliaoUrl(value)) throw new CaoliaoException("CAOLIAO_PAGE_FETCH", "CAOLIAO_PAGE_FETCH：草料内容页获取失败：" + safe(e.getMessage()));
            throw e;
        }
        String type = detector.detect(response.contentType, fileName(response.disposition, response.url), response.bytes);
        if (!"HTML".equals(type)) {
            String name = fileName(response.disposition, response.url);
            return Target.direct(response.url, new RecruitmentAttachmentDraft(name, response.url, type, classifier.attachmentType(name), "二维码直接附件"));
        }
        Document document = Jsoup.parse(new String(response.bytes, StandardCharsets.UTF_8), response.url);
        List<RecruitmentAttachmentDraft> files = list(document, response.url);
        if (files.isEmpty() && caoliao(document, response.url)) {
            Response markdown;
            try {
                markdown = fetch(caoliaoMarkdownUrl(response.url), MAX_RESPONSE_BYTES, context);
            } catch (Exception e) {
                throw new CaoliaoException("CAOLIAO_FILE_LIST_EXTRACT", "CAOLIAO_FILE_LIST_EXTRACT：草料文件清单获取失败：" + safe(e.getMessage()));
            }
            files = markdownList(new String(markdown.bytes, StandardCharsets.UTF_8), response.url, markdown.url);
            return files.isEmpty() ? Target.caoliaoUnresolved(response.url) : Target.list(response.url, files);
        }
        return files.isEmpty() ? Target.nonAttachment(response.url) : Target.list(response.url, files);
    }

    private List<RecruitmentAttachmentDraft> list(Document document, String sourceUrl) {
        Map<String, RecruitmentAttachmentDraft> result = new LinkedHashMap<>();
        for (Element element : document.select("a[href], [data-url], [data-href], [data-file]")) {
            String href = first(element, "href", "data-url", "data-href", "data-file");
            if (href == null || href.startsWith("#") || href.toLowerCase(Locale.ROOT).startsWith("javascript:")) continue;
            String url;
            try { url = external(URI.create(sourceUrl).resolve(href).normalize().toString()).toString(); }
            catch (Exception ignored) { continue; }
            String text = element.text().trim();
            String name = candidateName(text, url);
            String type = classifier.fileType(name);
            String category = classifier.attachmentType(name);
            if ("OTHER".equals(type) && "OTHER".equals(category) && !attachmentHint(text, url)) continue;
            result.putIfAbsent(url, new RecruitmentAttachmentDraft(name, url, type, category, "文件列表页：" + sourceUrl));
        }
        return new ArrayList<>(result.values());
    }

    private List<RecruitmentAttachmentDraft> markdownList(String markdown, String pageUrl, String markdownUrl) {
        Map<String, RecruitmentAttachmentDraft> result = new LinkedHashMap<>();
        Matcher matcher = MARKDOWN_LINK.matcher(markdown);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String url;
            try {
                url = external(matcher.group(2)).toString();
            } catch (Exception ignored) {
                continue;
            }
            String type = classifier.fileType(name);
            String category = classifier.attachmentType(name);
            if ("OTHER".equals(type) && "OTHER".equals(category) && !attachmentHint(name, url)) continue;
            String source = "草料文件清单：" + markdownUrl + "；草料内容页：" + pageUrl;
            result.putIfAbsent(url, new RecruitmentAttachmentDraft(name, url, type, category, source));
        }
        return new ArrayList<>(result.values());
    }

    private boolean caoliao(Document document, String url) {
        if (caoliaoUrl(url)) return true;
        return document.selectFirst("link[type=text/markdown]") != null
                && document.html().contains("clewm");
    }

    private boolean caoliaoUrl(String value) {
        try {
            String host = URI.create(value).getHost();
            return host != null && (host.endsWith("cli.im") || host.endsWith("clewm.net") || host.endsWith("qr61.cn"));
        } catch (Exception ignored) {
            return false;
        }
    }

    private String caoliaoMarkdownUrl(String value) {
        URI uri = URI.create(value);
        String query = uri.getRawQuery();
        if (query != null && Pattern.compile("(?:^|&)format=md(?:&|$)", Pattern.CASE_INSENSITIVE).matcher(query).find()) return uri.toString();
        String appended = query == null || query.isEmpty() ? "format=md" : query + "&format=md";
        try {
            return new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), appended, uri.getFragment()).toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("草料文件页地址无效", e);
        }
    }

    private boolean attachmentHint(String text, String url) {
        String value = (text + " " + url).toLowerCase(Locale.ROOT);
        return value.contains("附件") || value.contains("下载") || value.contains("download") || value.contains("file");
    }

    private String candidateName(String text, String url) {
        if (text != null && !text.trim().isEmpty() && !text.matches("(?i).*^(下载|download|查看)$")) return text.trim();
        return fileName(null, url);
    }

    private Response fetch(String value, int maxBytes) throws Exception {
        return fetch(value, maxBytes, new FetchContext());
    }

    private Response fetch(String value, int maxBytes, FetchContext context) throws Exception {
        URL current = external(value).toURL();
        for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
            HttpURLConnection connection = RecruitmentTrustedExternalConnection.open(current);
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (context.referer != null) connection.setRequestProperty("Referer", context.referer);
            if (!context.cookies.isEmpty()) connection.setRequestProperty("Cookie", cookieLine(context.cookies));
            int status = connection.getResponseCode();
            cookies(connection, context.cookies);
            if (redirect(status)) {
                if (redirect == MAX_REDIRECTS) throw new QrRestrictedException("二维码目标重定向次数超过限制");
                String location = connection.getHeaderField("Location");
                if (location == null || location.trim().isEmpty()) throw new QrRestrictedException("二维码目标重定向地址无效");
                current = external(current.toURI().resolve(location).normalize().toString()).toURL();
                continue;
            }
            if (status < 200 || status >= 300) throw new IllegalStateException("二维码目标返回 HTTP " + status);
            context.referer = current.toString();
            return new Response(current.toString(), connection.getContentType(), connection.getHeaderField("Content-Disposition"), read(connection.getInputStream(), maxBytes));
        }
        throw new QrRestrictedException("二维码目标地址无效");
    }

    private URI external(String value) throws Exception {
        URI uri = URI.create(value.trim()).normalize();
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) throw new QrRestrictedException("仅允许 HTTP 或 HTTPS 二维码目标");
        if (uri.getHost() == null || uri.getUserInfo() != null) throw new QrRestrictedException("二维码目标地址无效");
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) if (!RecruitmentNoticeImageService.publicAddress(address)) throw new QrRestrictedException("二维码目标不允许访问内网或本机地址");
        return uri;
    }

    private byte[] read(InputStream input, int max) throws Exception {
        try (InputStream in = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int total = 0, count;
            while ((count = in.read(buffer)) != -1) {
                total += count;
                if (total > max) throw new QrRestrictedException("二维码目标响应超过大小限制");
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private void mark(RecruitmentAttachment hint, String status, String message) {
        attachments.updateParseResult(hint.getId(), hint.getFileType(), status, safe(message), 0, LocalDateTime.now());
    }
    private void cookies(HttpURLConnection connection, Map<String, String> jar) { for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) if (header.getKey() != null && "Set-Cookie".equalsIgnoreCase(header.getKey())) for (String value : header.getValue()) { int end = value.indexOf(';'), split = value.indexOf('='); if (split > 0) jar.put(value.substring(0, split), value.substring(split + 1, end < 0 ? value.length() : end)); } }
    private String cookieLine(Map<String, String> jar) { StringBuilder result = new StringBuilder(); for (Map.Entry<String, String> entry : jar.entrySet()) { if (result.length() > 0) result.append("; "); result.append(entry.getKey()).append('=').append(entry.getValue()); } return result.toString(); }
    private boolean redirect(int status) { return status == 301 || status == 302 || status == 303 || status == 307 || status == 308; }
    private String first(Element element, String... names) { for (String name : names) if (element.hasAttr(name) && !element.attr(name).trim().isEmpty()) return element.attr(name).trim(); return null; }
    private String fileName(String disposition, String url) { try { Matcher matcher = Pattern.compile("filename\\*?=(?:UTF-8''|\\\")?([^;\\\"]+)", Pattern.CASE_INSENSITIVE).matcher(disposition == null ? "" : disposition); if (matcher.find()) return URLDecoder.decode(matcher.group(1).trim(), "UTF-8"); String path = URI.create(url).getPath(); String name = path.substring(path.lastIndexOf('/') + 1); return name.isEmpty() ? "附件" : URLDecoder.decode(name, "UTF-8"); } catch (Exception ignored) { return "附件"; } }
    private String safe(String message) { return message == null || message.trim().isEmpty() ? "二维码处理失败" : message.length() > 450 ? message.substring(0, 450) : message; }

    public static class Resolution { public int qrCount, decodedCount, listPageCount, attachmentCount, positionDataCount, warningCount, caoliaoUnresolvedCount; }
    private enum Kind { DIRECT_ATTACHMENT, LIST_PAGE, NON_ATTACHMENT, CAOLIAO_UNRESOLVED }
    private static class Target { final Kind kind; final String sourceUrl; final List<RecruitmentAttachmentDraft> attachments; Target(Kind kind, String sourceUrl, List<RecruitmentAttachmentDraft> attachments) { this.kind = kind; this.sourceUrl = sourceUrl; this.attachments = attachments; } static Target direct(String url, RecruitmentAttachmentDraft attachment) { return new Target(Kind.DIRECT_ATTACHMENT, url, Collections.singletonList(attachment)); } static Target list(String url, List<RecruitmentAttachmentDraft> attachments) { return new Target(Kind.LIST_PAGE, url, attachments); } static Target nonAttachment(String url) { return new Target(Kind.NON_ATTACHMENT, url, Collections.<RecruitmentAttachmentDraft>emptyList()); } static Target caoliaoUnresolved(String url) { return new Target(Kind.CAOLIAO_UNRESOLVED, url, Collections.<RecruitmentAttachmentDraft>emptyList()); } }
    private static class Response { final String url, contentType, disposition; final byte[] bytes; Response(String url, String contentType, String disposition, byte[] bytes) { this.url = url; this.contentType = contentType; this.disposition = disposition; this.bytes = bytes; } }
    private static class FetchContext { final Map<String, String> cookies = new LinkedHashMap<>(); String referer; }
    private static class QrRestrictedException extends Exception { QrRestrictedException(String message) { super(message); } }
    private static class CaoliaoException extends Exception { final String stage; CaoliaoException(String stage, String message) { super(message); this.stage = stage; } }
}
