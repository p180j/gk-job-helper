package com.gk.jobhelper.service;

import com.gk.jobhelper.dto.RecruitmentNoticeVO;
import java.net.InetAddress;
import java.net.URLDecoder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecruitmentBodyImagesTest {
    private static final String PAGE = "https://example.com/news/2026/notice.html";


    @Test
    @org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "recruitment.images.live", matches = "true")
    void verifiesThreeCurrentOfficialNotices() throws Exception {
        String[] paths = {"2094700651801554944", "2095067642689302528", "2095066800624054272"};
        long[] ids = {18L, 20L, 21L};
        for (int i = 0; i < paths.length; i++) {
            RecruitmentNoticeVO notice = new RecruitmentNoticeVO();
            notice.setId(ids[i]);
            notice.setNoticeUrl("https://gzw.jiangxi.gov.cn/jxsgzw/rczp/content/content_" + paths[i] + ".html");
            JiangxiSasacRecruitmentDetailFetcher fetcher = new JiangxiSasacRecruitmentDetailFetcher(new RecruitmentAttachmentClassifier());
            Element raw = fetcher.findContent(Jsoup.connect(notice.getNoticeUrl()).userAgent("Mozilla/5.0").timeout(20000).get());
            System.out.println("LIVE_ORIGINAL id=" + ids[i] + " images=" + raw.select("img"));
            RecruitmentDetailFetchResult fetched = fetcher.fetch(notice);
            notice.setBodyHtml(fetched.bodyHtml);
            notice.setBodyText(fetched.bodyText);
            RecruitmentNoticeService notices = org.mockito.Mockito.mock(RecruitmentNoticeService.class);
            org.mockito.Mockito.when(notices.detail(ids[i])).thenReturn(notice);
            RecruitmentNoticeImageService service = new RecruitmentNoticeImageService(notices);
            Element rendered = Jsoup.parseBodyFragment(RecruitmentBodyImages.proxy(fetched.bodyHtml, notice.getNoticeUrl(), ids[i])).body();
            assertFalse(rendered.select("img").isEmpty());
            for (Element image : rendered.select("img")) {
                String source = image.attr("src");
                assertTrue(source.startsWith("/api/recruitment/notices/" + ids[i] + "/image?url="));
                RecruitmentNoticeImageService.Image loaded = service.load(ids[i], URLDecoder.decode(source.substring(source.indexOf("?url=") + 5), "UTF-8"));
                assertTrue(loaded.bytes.length > 0);
                assertTrue(loaded.type.startsWith("image/"));
                System.out.println("LIVE_IMAGE id=" + ids[i] + " mime=" + loaded.type + " bytes=" + loaded.bytes.length);
            }
            System.out.println("LIVE_RESULT id=" + ids[i] + " images=" + rendered.select("img").size() + " qrHints=" + fetched.attachments.stream().filter(a -> "QR_ATTACHMENT_HINT".equals(a.attachmentType)).count() + " candidate=" + notice.getBodyPositionHint());
        }
    }

    @Test
    void resolvesEveryImageSourceAndEncodesProxyParameters() throws Exception {
        String[] attributes = {"src", "data-src", "data-original", "data-url"};
        String[] sources = {"https://example.com/a.png", "//example.com/b.png", "/c.png", "../d.png?q=1&name=a+b"};
        String[] expected = {"https://example.com/a.png", "https://example.com/b.png", "https://example.com/c.png", "https://example.com/news/d.png?q=1&name=a+b"};
        for (int i = 0; i < attributes.length; i++) {
            String html = "<picture><source srcset='/uncontrolled.png'><img " + attributes[i] + "='" + sources[i] + "' srcset='/uncontrolled.png'></picture>";
            Element body = Jsoup.parseBodyFragment(RecruitmentBodyImages.proxy(html, PAGE, 23L)).body();
            Element image = body.selectFirst("img");
            String prefix = "/api/recruitment/notices/23/image?url=";
            assertTrue(image.attr("src").startsWith(prefix));
            assertEquals(expected[i], URLDecoder.decode(image.attr("src").substring(prefix.length()), "UTF-8"));
            assertFalse(image.hasAttr("srcset"));
            assertFalse(image.hasAttr("data-src"));
            assertTrue(body.select("source").isEmpty());
        }
    }

    @Test
    void restoresLazyImageInsteadOfPlaceholderAndSkipsInvalidLazyValue() {
        Element body = Jsoup.parseBodyFragment("<img src='data:image/gif;base64,AA' data-src='../qr.png'><img data-src='javascript:alert(1)' data-original='/real.png'>").body();
        RecruitmentBodyImages.normalize(body, PAGE);
        assertEquals("https://example.com/news/qr.png", body.select("img").get(0).attr("src"));
        assertEquals("https://example.com/real.png", body.select("img").get(1).attr("src"));
    }

    @Test
    void rejectsUnsafeImageSourcesEvenWhenNoticeUsesSameHost() {
        for (String host : new String[]{"localhost", "127.0.0.1", "10.0.0.1", "172.16.0.1", "192.168.1.1", "169.254.169.254", "100.64.0.1", "[::1]", "[fc00::1]"}) {
            assertThrows(Exception.class, () -> RecruitmentNoticeImageService.validate("https://" + host + "/image", "https://" + host + "/notice"));
        }
        assertThrows(Exception.class, () -> RecruitmentNoticeImageService.validate("http://example.com/image", PAGE));
        assertThrows(Exception.class, () -> RecruitmentNoticeImageService.validate("https://other.example.com/image", PAGE));
        assertThrows(Exception.class, () -> RecruitmentNoticeImageService.validate("https://user@example.com/image", PAGE));
        assertThrows(Exception.class, () -> RecruitmentNoticeImageService.validate("https://example.com:8443/image", PAGE));
    }

    @Test
    void allowsPublicAddressesAndOnlyMarksBodyCandidates() throws Exception {
        assertTrue(RecruitmentNoticeImageService.publicAddress(InetAddress.getByName("8.8.8.8")));
        RecruitmentNoticeVO notice = new RecruitmentNoticeVO();
        notice.setBodyHtml("<table><tr><td>信息</td></tr></table>");
        assertEquals("BODY_POSITION_CANDIDATE", notice.getBodyPositionHint());
        notice.setBodyHtml("<p>公告</p>");
        notice.setBodyText("岗位职责：负责日常工作");
        assertEquals("BODY_POSITION_CANDIDATE", notice.getBodyPositionHint());
        notice.setBodyText("考试时间变更通知");
        assertNull(notice.getBodyPositionHint());
    }
}
