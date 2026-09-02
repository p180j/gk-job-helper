package com.gk.jobhelper.service;

import java.io.InputStream;

/** 把已校验的简历文件转换为纯文本。 */
public interface ResumeTextExtractor {
    boolean supports(String fileName, String contentType);
    String extract(InputStream inputStream);
}
