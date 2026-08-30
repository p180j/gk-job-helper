package com.gk.jobhelper.service;
import com.fasterxml.jackson.databind.ObjectMapper;import com.gk.jobhelper.entity.JobPosition;import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
class ExamSubjectResolverTest {
 private final ExamSubjectResolver resolver=new ExamSubjectResolver(new ObjectMapper());
 @Test void shouldKeepDifferentShenlunPapersInDifferentGroups(){JobPosition a=job("考两科","《行政职业能力测验》,《申论》（省市综合管理类）");JobPosition b=job("2","行政职业能力测验，申论（行政执法类）");ExamSubjectResolver.Result ra=resolver.resolve(a),rb=resolver.resolve(b);assertEquals("RECOGNIZED",ra.getStatus());assertEquals(2,ra.getCount());assertNotEquals(ra.getGroup(),rb.getGroup());assertTrue(rb.getGroup().contains("行政执法类"));}
 @Test void missingSubjectsShouldBeUnknown(){ExamSubjectResolver.Result r=resolver.resolve(job(null,null));assertEquals("UNKNOWN",r.getStatus());assertNull(r.getGroup());}
 @Test void inconsistentCountShouldBeUncertain(){ExamSubjectResolver.Result r=resolver.resolve(job("3","行测,申论（行政执法类）"));assertEquals("UNCERTAIN",r.getStatus());assertNull(r.getGroup());}
 private JobPosition job(String count,String subjects){JobPosition p=new JobPosition();try{java.util.Map<String,String>m=new java.util.LinkedHashMap<>();if(count!=null)m.put("科目数量",count);if(subjects!=null)m.put("考试科目",subjects);p.setRawData(new ObjectMapper().writeValueAsString(m));}catch(Exception ignored){}return p;}
}
