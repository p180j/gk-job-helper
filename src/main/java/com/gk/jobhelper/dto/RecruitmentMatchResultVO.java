package com.gk.jobhelper.dto;

import com.gk.jobhelper.entity.RecruitmentMatchItem;
import com.gk.jobhelper.entity.RecruitmentMatchResult;
import java.util.*;

public class RecruitmentMatchResultVO extends RecruitmentMatchResult {
    private List<RecruitmentMatchItem> items=Collections.emptyList();
    public List<RecruitmentMatchItem> getItems(){return items;}
    public void setItems(List<RecruitmentMatchItem> v){items=v==null?Collections.emptyList():v;}
}
