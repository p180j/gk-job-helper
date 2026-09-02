package com.gk.jobhelper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gk.jobhelper.ai.AiClient;
import com.gk.jobhelper.ai.AiClientFactory;
import com.gk.jobhelper.ai.AiProviderConfig;
import com.gk.jobhelper.ai.AiRequest;
import com.gk.jobhelper.ai.AiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.dto.CareerProfileDraftVO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeServiceTest {
    @Test
    void shouldExtractDocxAndReturnAiDraft() throws Exception {
        ResumeService service = serviceWithResponse("{\"currentPosition\":\"Java开发工程师\",\"totalWorkYears\":\"8年\",\"careerDirections\":[\"后端开发\"],\"industries\":[\"金融科技\"],\"educationExperiences\":[],\"workExperiences\":[],\"projectExperiences\":[{\"name\":\"支付系统\",\"role\":\"开发\",\"startDate\":\"2021\",\"endDate\":\"2021-12\",\"description\":\"支付系统开发\"}],\"skills\":[\"Spring Boot\"],\"certificates\":[]}");
        MockMultipartFile file = new MockMultipartFile("file", "resume.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docx("使用 Spring Boot 开发系统"));

        CareerProfileDraftVO draft = service.parse(file, config());

        assertEquals("resume.docx", draft.getFileName());
        assertEquals("Java开发工程师", draft.getCurrentPosition());
        assertEquals("8年", draft.getTotalWorkYears());
        assertEquals("2021", draft.getProjectExperiences().get(0).getStartDate());
        assertEquals("2021-12", draft.getProjectExperiences().get(0).getEndDate());
        assertEquals(Collections.singletonList("Spring Boot"), draft.getSkills());
    }

    @Test
    void shouldExtractPdfText() throws Exception {
        PdfResumeTextExtractor extractor = new PdfResumeTextExtractor();
        String text = extractor.extract(new java.io.ByteArrayInputStream(pdf("Java Developer")));
        assertTrue(text.contains("Java Developer"));
    }

    @Test
    void shouldRejectMismatchedFileContent() {
        ResumeService service = serviceWithResponse("{}");
        MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", "not a pdf".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        BusinessException exception = assertThrows(BusinessException.class, () -> service.parse(file, config()));
        assertTrue(exception.getMessage().contains("不是有效 PDF"));
    }

    private ResumeService serviceWithResponse(final String response) {
        AiClient client = new AiClient() {
            @Override public String provider() { return "DEEPSEEK"; }
            @Override public AiResponse chat(AiProviderConfig config, AiRequest request) { return new AiResponse(response); }
        };
        return new ResumeService(Arrays.<ResumeTextExtractor>asList(new PdfResumeTextExtractor(), new DocxResumeTextExtractor()),
                new AiClientFactory(Collections.singletonList(client)), new ObjectMapper());
    }

    private AiProviderConfig config() {
        AiProviderConfig config = new AiProviderConfig();
        config.setProvider("DEEPSEEK"); config.setModel("deepseek-chat"); config.setBaseUrl("https://api.deepseek.com"); config.setApiKey("test-key");
        return config;
    }

    private byte[] docx(String text) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            XWPFParagraph paragraph = document.createParagraph();
            paragraph.createRun().setText(text);
            document.write(output);
            return output.toByteArray();
        }
    }

    private byte[] pdf(String text) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(); document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText(); content.setFont(PDType1Font.HELVETICA, 12); content.newLineAtOffset(100, 700); content.showText(text); content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
