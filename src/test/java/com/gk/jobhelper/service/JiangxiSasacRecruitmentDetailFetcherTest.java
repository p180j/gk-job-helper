package com.gk.jobhelper.service;

import java.lang.reflect.Method;
import java.util.List;
import com.gk.jobhelper.mapper.RecruitmentAttachmentMapper;
import com.gk.jobhelper.mapper.RecruitmentNoticeMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;

class JiangxiSasacRecruitmentDetailFetcherTest {
    @Test
    void keepsDifferentAttachmentNamesThatShareOneDownloadUrl() throws Exception {
        String html = "<div><a href=\"/download?id=123\">国盛证券股份有限公司总部2026年社会招聘岗位明细表（第十批）</a>"
                + "<a href=\"/download?id=123\">应聘人员诚信承诺书</a>"
                + "<a href=\"/navigation\">返回首页</a></div>";
        JiangxiSasacRecruitmentDetailFetcher fetcher = new JiangxiSasacRecruitmentDetailFetcher(new RecruitmentAttachmentClassifier());
        Method method = JiangxiSasacRecruitmentDetailFetcher.class.getDeclaredMethod("attachments", Element.class, String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<RecruitmentAttachmentDraft> attachments = (List<RecruitmentAttachmentDraft>) method.invoke(fetcher, Jsoup.parse(html).body(), "https://example.com/a/b.html");

        assertEquals(2, attachments.size());
        assertEquals("POSITION_DATA", attachments.get(0).attachmentType);
        assertEquals("COMMITMENT", attachments.get(1).attachmentType);
        assertEquals("OTHER", attachments.get(0).fileType);
        assertEquals("OTHER", attachments.get(1).fileType);
        assertEquals("https://example.com/download?id=123", attachments.get(0).fileUrl);
        assertEquals("https://example.com/download?id=123", attachments.get(1).fileUrl);
    }

    @Test
    void createsStableDedupeKeyForRepeatedSaveAndDifferentKeyForDifferentAttachmentName() throws Exception {
        RecruitmentDetailService service = new RecruitmentDetailService(mock(RecruitmentNoticeService.class), mock(RecruitmentNoticeMapper.class), mock(RecruitmentAttachmentMapper.class), java.util.Collections.emptyList());
        Method method = RecruitmentDetailService.class.getDeclaredMethod("key", String.class, String.class);
        method.setAccessible(true);
        String url = "https://example.com/download?id=123";
        String positionKey = (String) method.invoke(service, url, "招聘岗位明细表");
        String repeatedPositionKey = (String) method.invoke(service, url, "招聘岗位明细表");
        String commitmentKey = (String) method.invoke(service, url, "应聘人员诚信承诺书");

        assertEquals(positionKey, repeatedPositionKey);
        assertNotEquals(positionKey, commitmentKey);
    }
}
