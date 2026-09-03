package com.gk.jobhelper.service;

import com.gk.jobhelper.config.UploadProperties;
import com.gk.jobhelper.entity.ResumeFile;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.mapper.ResumeFileMapper;
import com.gk.jobhelper.mapper.UserProfileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumeFileServiceTest {
    @TempDir Path tempDir;

    @Test
    void shouldSaveCurrentResumeMetadataAndOriginalFile() throws Exception {
        ResumeFileMapper resumes = mock(ResumeFileMapper.class);
        UserProfileMapper profiles = mock(UserProfileMapper.class);
        UserProfile profile = new UserProfile(); profile.setId(1L);
        when(profiles.selectFirstProfile()).thenReturn(profile);
        when(resumes.selectByProfileId(anyLong())).thenReturn(null);
        UploadProperties properties = new UploadProperties(); properties.setDir(tempDir.toString());

        ResumeFileService service = new ResumeFileService(resumes, profiles, properties);
        service.saveCurrent("我的简历.pdf", "application/pdf", "%PDF-test".getBytes("UTF-8"));

        org.mockito.ArgumentCaptor<ResumeFile> captor = org.mockito.ArgumentCaptor.forClass(ResumeFile.class);
        verify(resumes).insert(captor.capture());
        ResumeFile saved = captor.getValue();
        assertEquals("我的简历.pdf", saved.getOriginalFilename());
        assertEquals("application/pdf", saved.getFileType());
        assertEquals(9L, saved.getFileSize().longValue());
        assertTrue(Files.exists(Paths.get(saved.getStoragePath())));
    }
}
