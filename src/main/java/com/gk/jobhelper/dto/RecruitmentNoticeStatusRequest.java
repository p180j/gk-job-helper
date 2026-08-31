package com.gk.jobhelper.dto;
import javax.validation.constraints.NotBlank;
public class RecruitmentNoticeStatusRequest { @NotBlank private String status; public String getStatus(){return status;}public void setStatus(String v){status=v;} }
