package com.gk.jobhelper.service;
import com.gk.jobhelper.entity.RecruitmentPosition;import java.util.*;
public class RecruitmentPositionExtractionResult {public final List<RecruitmentPosition> positions;public final int sheetCount;public final Map<String,Integer> headerRows;public RecruitmentPositionExtractionResult(List<RecruitmentPosition> p,int sheets,Map<String,Integer> headers){positions=p;sheetCount=sheets;headerRows=headers;}}
