package com.gk.jobhelper.dto;

import java.time.LocalDateTime;

/** 已由用户确认保存的职业画像。 */
public class CareerProfileVO extends CareerProfileDraftVO {
    private Long id;
    private Long profileId;
    private LocalDateTime updatedAt;
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
