package com.gk.jobhelper.service;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import com.gk.jobhelper.config.UploadProperties;
import com.gk.jobhelper.dto.ResumeFileVO;
import com.gk.jobhelper.entity.ResumeFile;
import com.gk.jobhelper.entity.UserProfile;
import com.gk.jobhelper.mapper.ResumeFileMapper;
import com.gk.jobhelper.mapper.UserProfileMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ResumeFileService {
    private final ResumeFileMapper resumeFileMapper;
    private final UserProfileMapper userProfileMapper;
    private final UploadProperties uploadProperties;

    public ResumeFileService(ResumeFileMapper resumeFileMapper, UserProfileMapper userProfileMapper, UploadProperties uploadProperties) {
        this.resumeFileMapper = resumeFileMapper; this.userProfileMapper = userProfileMapper; this.uploadProperties = uploadProperties;
    }

    @Transactional
    public ResumeFile saveCurrent(String fileName, String fileType, byte[] content) {
        UserProfile profile = requireProfile();
        ResumeFile previous = resumeFileMapper.selectByProfileId(profile.getId());
        Path stored = store(fileName, content);
        LocalDateTime now = LocalDateTime.now();
        ResumeFile current = new ResumeFile();
        current.setProfileId(profile.getId()); current.setOriginalFilename(fileName); current.setFileType(fileType);
        current.setFileSize((long) content.length); current.setStoragePath(stored.toString()); current.setUploadedAt(now); current.setUpdatedAt(now);
        if (previous == null) resumeFileMapper.insert(current); else resumeFileMapper.updateByProfileId(current);
        if (previous != null) deleteQuietly(previous.getStoragePath());
        return resumeFileMapper.selectByProfileId(profile.getId());
    }

    public ResumeFileVO getCurrent() {
        UserProfile profile = userProfileMapper.selectFirstProfile();
        if (profile == null) return null;
        return toVO(resumeFileMapper.selectByProfileId(profile.getId()));
    }

    public ResumeFile currentForDownload() {
        UserProfile profile = requireProfile();
        ResumeFile resume = resumeFileMapper.selectByProfileId(profile.getId());
        if (resume == null) throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "暂无已保存的原始简历");
        if (!new FileSystemResource(resume.getStoragePath()).exists()) throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "已保存的简历文件不存在，请重新上传");
        return resume;
    }

    private UserProfile requireProfile() {
        UserProfile profile = userProfileMapper.selectFirstProfile();
        if (profile == null) throw new BusinessException(ApiResponse.CODE_PROFILE_NOT_FOUND, "请先保存我的档案，再上传简历。");
        return profile;
    }

    private Path store(String fileName, byte[] content) {
        try {
            String suffix = fileName.substring(fileName.lastIndexOf('.')).toLowerCase(java.util.Locale.ROOT);
            Path dir = Paths.get(uploadProperties.getDir()).toAbsolutePath().normalize().resolve("resumes");
            Files.createDirectories(dir);
            Path target = dir.resolve(UUID.randomUUID().toString().replace("-", "") + suffix);
            Files.write(target, content);
            return target;
        } catch (IOException e) { throw new BusinessException("保存原始简历文件失败，请重新上传。"); }
    }

    private ResumeFileVO toVO(ResumeFile resume) {
        if (resume == null) return null;
        ResumeFileVO vo = new ResumeFileVO(); vo.setResumeId(resume.getId()); vo.setOriginalFilename(resume.getOriginalFilename());
        vo.setFileType(resume.getFileType()); vo.setFileSize(resume.getFileSize()); vo.setUploadedAt(resume.getUploadedAt()); return vo;
    }

    private void deleteQuietly(String path) { try { if (path != null) Files.deleteIfExists(Paths.get(path)); } catch (IOException ignored) { } }
}
