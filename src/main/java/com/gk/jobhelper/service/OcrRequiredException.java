package com.gk.jobhelper.service;

public class OcrRequiredException extends RuntimeException {
    public OcrRequiredException() { super("该附件为扫描件，当前版本暂不支持自动识别，可打开原附件查看。"); }
}
