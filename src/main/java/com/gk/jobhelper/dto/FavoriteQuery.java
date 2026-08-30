package com.gk.jobhelper.dto;

public class FavoriteQuery {
    private Long profileId;
    private int offset;
    private int size;

    public Long getProfileId() { return profileId; }
    public void setProfileId(Long profileId) { this.profileId = profileId; }
    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
}
