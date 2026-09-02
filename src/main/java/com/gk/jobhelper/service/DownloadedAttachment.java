package com.gk.jobhelper.service;

public class DownloadedAttachment {
    public final Long attachmentId;
    public final String fileName, contentType, fileType, originalUrl, finalUrl, contentDisposition;
    public final byte[] bytes;
    public final int httpStatus, redirectCount, relayPageCount;
    public DownloadedAttachment(Long id, String name, String type, String detected, byte[] data) { this(id, name, type, detected, data, null, null, 200, null, 0, 0); }
    public DownloadedAttachment(Long id, String name, String type, String detected, byte[] data, String original, String finalUrl, int status, String disposition, int redirects, int relays) { attachmentId=id;fileName=name;contentType=type;fileType=detected;bytes=data;originalUrl=original;this.finalUrl=finalUrl;httpStatus=status;contentDisposition=disposition;redirectCount=redirects;relayPageCount=relays; }
    public Long getAttachmentId(){return attachmentId;} public String getFileName(){return fileName;} public String getContentType(){return contentType;} public String getFileType(){return fileType;} public byte[] getBytes(){return bytes;}
}
