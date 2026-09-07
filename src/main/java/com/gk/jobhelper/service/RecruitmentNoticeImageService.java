package com.gk.jobhelper.service;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.RecruitmentNoticeVO;
import java.io.*;
import java.net.*;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class RecruitmentNoticeImageService {
    private final RecruitmentNoticeService notices;
    public RecruitmentNoticeImageService(RecruitmentNoticeService notices) { this.notices = notices; }

    public Image load(Long id, String url) {
        RecruitmentNoticeVO notice = notices.detail(id);
        HttpURLConnection connection = null;
        try {
            URL image = validate(url, notice.getNoticeUrl());
            connection = (HttpURLConnection) image.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestProperty("Referer", notice.getNoticeUrl());
            if (connection.getResponseCode() != 200) throw invalid("公告图片暂时无法访问");
            String type = connection.getContentType();
            String mime = type == null ? "" : type.split(";")[0].trim().toLowerCase(Locale.ROOT);
            if (!java.util.Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/avif", "image/x-icon").contains(mime))
                throw invalid("公告资源不是支持的图片");
            return new Image(mime, read(connection.getInputStream()));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw invalid("公告图片暂时无法访问");
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    static URL validate(String value, String noticeUrl) throws IOException {
        URL image = new URL(value), page = new URL(noticeUrl);
        String host = image.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(image.getProtocol()) || !host.equalsIgnoreCase(page.getHost())
                || image.getUserInfo() != null || (image.getPort() != -1 && image.getPort() != 443)
                || host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local"))
            throw invalid("图片来源无效");
        for (InetAddress address : InetAddress.getAllByName(host)) {
            if (!publicAddress(address)) throw invalid("图片来源无效");
        }
        return image;
    }

    static boolean publicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
        byte[] bytes = address.getAddress();
        if (bytes.length == 16) return (bytes[0] & 0xe0) == 0x20;
        int a = bytes[0] & 255, b = bytes[1] & 255;
        return a != 0 && a < 224 && !(a == 100 && b >= 64 && b <= 127)
                && !(a == 169 && b == 254) && !(a == 198 && (b == 18 || b == 19));
    }

    private byte[] read(InputStream input) throws IOException {
        try (InputStream in = input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count, total = 0;
            while ((count = in.read(buffer)) != -1) {
                if ((total += count) > 5 * 1024 * 1024) throw invalid("公告图片超过5MB限制");
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        }
    }

    private static BusinessException invalid(String message) { return new BusinessException(ApiResponse.CODE_BAD_REQUEST, message); }
    public static class Image {
        public final String type;
        public final byte[] bytes;
        Image(String type, byte[] bytes) { this.type = type; this.bytes = bytes; }
    }
}
