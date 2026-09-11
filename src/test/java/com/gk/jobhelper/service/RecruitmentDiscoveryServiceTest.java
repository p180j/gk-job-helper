package com.gk.jobhelper.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.gk.jobhelper.dto.RecruitmentNoticeCandidate;
import com.gk.jobhelper.mapper.RecruitmentNoticeMapper;
import com.gk.jobhelper.mapper.RecruitmentSourceMapper;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class RecruitmentDiscoveryServiceTest {
    @Test
    void recentRangeOnlyCreatesCandidatesPublishedWithinSelectedCalendarDays() {
        RecruitmentSourceMapper sources = mock(RecruitmentSourceMapper.class);
        RecruitmentNoticeMapper notices = mock(RecruitmentNoticeMapper.class);
        RecruitmentAutoProcessService autoProcess = mock(RecruitmentAutoProcessService.class);
        com.gk.jobhelper.entity.RecruitmentSource source = new com.gk.jobhelper.entity.RecruitmentSource(); source.setId(1L); source.setSourceCode("TEST");
        when(sources.selectEnabled()).thenReturn(Arrays.asList(source));
        RecruitmentSource adapter = new RecruitmentSource() {
            public String getSourceCode() { return "TEST"; }
            public String getSourceName() { return "测试来源"; }
            public java.util.List<RecruitmentNoticeCandidate> fetchLatest() { return Arrays.asList(
                    new RecruitmentNoticeCandidate("今天", "https://example.test/today", LocalDateTime.now()),
                    new RecruitmentNoticeCandidate("过期", "https://example.test/old", LocalDateTime.now().minusDays(5)),
                    new RecruitmentNoticeCandidate("日期未知", "https://example.test/unknown", null)); }
        };
        RecruitmentDiscoveryService service = new RecruitmentDiscoveryService(sources, notices, autoProcess, Arrays.asList(adapter));

        assertEquals(1, service.discoverAll(3).getNewCount());
        assertEquals(2, service.discoverAll(3).getFilteredCount());
        verify(notices, times(2)).insert(any());
        verify(autoProcess, times(2)).processNoticeAsync(any());
        verify(notices, never()).selectBySourceAndUrl(eq(1L), eq("https://example.test/old"));
    }
}
