package com.gk.jobhelper.dto;
import com.gk.jobhelper.entity.RecruitmentPosition;import com.gk.jobhelper.entity.RecruitmentRequirement;import java.util.*;
public class RecruitmentPositionVO extends RecruitmentPosition {private List<RecruitmentRequirement> requirements=Collections.emptyList();public List<RecruitmentRequirement> getRequirements(){return requirements;}public void setRequirements(List<RecruitmentRequirement> v){requirements=v==null?Collections.emptyList():v;}}
