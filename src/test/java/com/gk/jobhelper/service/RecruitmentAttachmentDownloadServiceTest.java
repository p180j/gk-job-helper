package com.gk.jobhelper.service;

import static org.junit.jupiter.api.Assertions.*;
import com.gk.jobhelper.entity.RecruitmentAttachment;
import com.sun.net.httpserver.*;
import java.io.*;import java.net.*;import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;

class RecruitmentAttachmentDownloadServiceTest {
 private HttpServer server; private String base; private final AtomicBoolean contextPassed=new AtomicBoolean();
 @BeforeEach void start()throws Exception{server=HttpServer.create(new InetSocketAddress(0),0);base="http://127.0.0.1:"+server.getAddress().getPort();server.createContext("/relay",this::relay);server.createContext("/file",this::file);server.start();}
 @AfterEach void stop(){server.stop(0);}
 @Test void resolvesHtmlRelayAndPassesCookieAndReferer()throws Exception{RecruitmentAttachment attachment=new RecruitmentAttachment();attachment.setId(1L);attachment.setFileName("岗位表");attachment.setFileUrl(base+"/relay");DownloadedAttachment result=new RecruitmentAttachmentDownloadService(new RecruitmentFileTypeDetector()).download(attachment);assertEquals("XLSX",result.fileType);assertEquals(base+"/file",result.finalUrl);assertEquals(1,result.relayPageCount);assertTrue(contextPassed.get());}
 private void relay(HttpExchange e)throws IOException{e.getResponseHeaders().add("Set-Cookie","relayToken=yes; Path=/");byte[] body="<a href='/file'>下载岗位表</a>".getBytes("UTF-8");e.getResponseHeaders().add("Content-Type","text/html;charset=UTF-8");e.sendResponseHeaders(200,body.length);e.getResponseBody().write(body);e.close();}
 private void file(HttpExchange e)throws IOException{String cookie=e.getRequestHeaders().getFirst("Cookie"),referer=e.getRequestHeaders().getFirst("Referer");contextPassed.set(cookie!=null&&cookie.contains("relayToken=yes")&&(base+"/relay").equals(referer));byte[] body=xlsx();e.getResponseHeaders().add("Content-Type","application/octet-stream");e.getResponseHeaders().add("Content-Disposition","attachment; filename=positions.xlsx");e.sendResponseHeaders(200,body.length);e.getResponseBody().write(body);e.close();}
 private byte[] xlsx()throws IOException{try(XSSFWorkbook wb=new XSSFWorkbook();ByteArrayOutputStream out=new ByteArrayOutputStream()){wb.createSheet("岗位").createRow(0).createCell(0).setCellValue("岗位名称");wb.write(out);return out.toByteArray();}}
}
