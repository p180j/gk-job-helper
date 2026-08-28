package com.gk.jobhelper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 上传文件保存目录配置（upload.dir），可在 application.yml 中覆盖
 */
@Component
@ConfigurationProperties(prefix = "upload")
public class UploadProperties {

    /** Excel 原始文件保存目录 */
    private String dir = "./uploads";

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
