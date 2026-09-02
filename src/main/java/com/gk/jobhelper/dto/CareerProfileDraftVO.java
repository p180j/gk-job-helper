package com.gk.jobhelper.dto;

import java.util.ArrayList;
import java.util.List;

/** AI 从简历提取的未保存草稿。 */
public class CareerProfileDraftVO {
    private String fileName;
    private String currentPosition;
    private String totalWorkYears;
    private List<String> careerDirections = new ArrayList<>();
    private List<String> industries = new ArrayList<>();
    private List<CareerEducationVO> educationExperiences = new ArrayList<>();
    private List<CareerWorkVO> workExperiences = new ArrayList<>();
    private List<CareerProjectVO> projectExperiences = new ArrayList<>();
    private List<String> skills = new ArrayList<>();
    private List<String> certificates = new ArrayList<>();
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getCurrentPosition() { return currentPosition; }
    public void setCurrentPosition(String currentPosition) { this.currentPosition = currentPosition; }
    public String getTotalWorkYears() { return totalWorkYears; }
    public void setTotalWorkYears(String totalWorkYears) { this.totalWorkYears = totalWorkYears; }
    public List<String> getCareerDirections() { return careerDirections; }
    public void setCareerDirections(List<String> value) { careerDirections = value == null ? new ArrayList<String>() : value; }
    public List<String> getIndustries() { return industries; }
    public void setIndustries(List<String> value) { industries = value == null ? new ArrayList<String>() : value; }
    public List<CareerEducationVO> getEducationExperiences() { return educationExperiences; }
    public void setEducationExperiences(List<CareerEducationVO> value) { educationExperiences = value == null ? new ArrayList<CareerEducationVO>() : value; }
    public List<CareerWorkVO> getWorkExperiences() { return workExperiences; }
    public void setWorkExperiences(List<CareerWorkVO> value) { workExperiences = value == null ? new ArrayList<CareerWorkVO>() : value; }
    public List<CareerProjectVO> getProjectExperiences() { return projectExperiences; }
    public void setProjectExperiences(List<CareerProjectVO> value) { projectExperiences = value == null ? new ArrayList<CareerProjectVO>() : value; }
    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> value) { skills = value == null ? new ArrayList<String>() : value; }
    public List<String> getCertificates() { return certificates; }
    public void setCertificates(List<String> value) { certificates = value == null ? new ArrayList<String>() : value; }
}
