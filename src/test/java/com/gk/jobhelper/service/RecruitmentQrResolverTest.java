package com.gk.jobhelper.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.gk.jobhelper.mapper.RecruitmentAttachmentMapper;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

class RecruitmentQrResolverTest {
    private final RecruitmentQrResolver resolver = new RecruitmentQrResolver(mock(RecruitmentAttachmentMapper.class),
            new RecruitmentAttachmentClassifier(), new RecruitmentFileTypeDetector(), mock(RecruitmentDetailService.class));

    @Test
    void decodesQrAndClassifiesOnlyPositionDataFromFileList() throws Exception {
        Method decode = RecruitmentQrResolver.class.getDeclaredMethod("decode", byte[].class);
        decode.setAccessible(true);
        assertEquals("https://www.example.com/files", decode.invoke(resolver, qr("https://www.example.com/files")));

        Method list = RecruitmentQrResolver.class.getDeclaredMethod("list", org.jsoup.nodes.Document.class, String.class);
        list.setAccessible(true);
        String html = "<a href='https://8.8.8.8/download?id=1'>2026年秋季招聘岗位计划表.xlsx</a>"
                + "<a href='https://8.8.8.8/apply.pdf'>招聘人员报名表.pdf</a>"
                + "<a href='https://8.8.8.8/commit.pdf'>诚信报名承诺书.pdf</a>"
                + "<a href='https://8.8.8.8/relative.pdf'>近亲属关系排查表.pdf</a>";
        @SuppressWarnings("unchecked") List<RecruitmentAttachmentDraft> files = (List<RecruitmentAttachmentDraft>) list.invoke(resolver, Jsoup.parse(html), "https://8.8.8.8/list");
        assertEquals(4, files.size());
        assertEquals("POSITION_DATA", files.get(0).attachmentType);
        assertEquals("XLSX", files.get(0).fileType);
        assertEquals("APPLICATION_FORM", files.get(1).attachmentType);
        assertEquals("COMMITMENT", files.get(2).attachmentType);
        assertEquals("QUALIFICATION_MATERIAL", files.get(3).attachmentType);
    }

    @Test
    void rejectsLocalAndPrivateQrTargetsBeforeRequestingThem() throws Exception {
        Method external = RecruitmentQrResolver.class.getDeclaredMethod("external", String.class);
        external.setAccessible(true);
        for (String url : new String[]{"http://127.0.0.1/a", "http://localhost/a", "http://192.168.1.1/a", "http://10.0.0.1/a"}) {
            InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> external.invoke(resolver, url));
            assertTrue(exception.getCause().getMessage().contains("内网") || exception.getCause().getMessage().contains("地址无效"));
        }
    }

    @Test
    void extractsCaoliaoMarkdownFileListAndKeepsOnlyPositionDataForPositionParsing() throws Exception {
        Method markdownList = RecruitmentQrResolver.class.getDeclaredMethod("markdownList", String.class, String.class, String.class);
        markdownList.setAccessible(true);
        String markdown = "文件:[2026年秋季招聘岗位计划表.xlsx](https://8.8.8.8/download?id=1)\n"
                + "文件:[招聘人员报名表.pdf](https://8.8.8.8/apply)\n"
                + "文件:[近亲属关系排查表.pdf](https://8.8.8.8/relative)\n"
                + "文件:[诚信报名承诺书.pdf](https://8.8.8.8/commit)";
        @SuppressWarnings("unchecked") List<RecruitmentAttachmentDraft> files = (List<RecruitmentAttachmentDraft>) markdownList.invoke(resolver,
                markdown, "https://qr61.cn/oD8mJN/q8ubS38", "https://qr61.cn/oD8mJN/q8ubS38?format=md");
        assertEquals(4, files.size());
        assertEquals("POSITION_DATA", files.get(0).attachmentType);
        assertEquals("XLSX", files.get(0).fileType);
        assertEquals("APPLICATION_FORM", files.get(1).attachmentType);
        assertEquals("QUALIFICATION_MATERIAL", files.get(2).attachmentType);
        assertEquals("COMMITMENT", files.get(3).attachmentType);
        assertTrue(files.get(0).sourceText.contains("草料文件清单"));
    }

    private byte[] qr(String value) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, 180, 180);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", output);
        return output.toByteArray();
    }
}
