package com.gk.jobhelper.dto;
import javax.validation.constraints.NotBlank;
public class RecruitmentSourceCreateRequest { @NotBlank(message="网站名称不能为空") private String sourceName; @NotBlank(message="招聘公告列表地址不能为空") private String listUrl; public String getSourceName(){return sourceName;}public void setSourceName(String v){sourceName=v;}public String getListUrl(){return listUrl;}public void setListUrl(String v){listUrl=v;} }
