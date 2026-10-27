package com.gk.jobhelper.dto;

import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only comparison of stored decisions with the current rules for one profile and import. */
public class MatchChangePreviewVO {
    private Long profileId, importId;
    private int stored, changedResults, changedMajorItems, changedRemarkItems;
    private final Map<String, Integer> transitions = new LinkedHashMap<>();
    public Long getProfileId() { return profileId; }
    public void setProfileId(Long value) { profileId = value; }
    public Long getImportId() { return importId; }
    public void setImportId(Long value) { importId = value; }
    public int getStored() { return stored; }
    public void setStored(int value) { stored = value; }
    public int getChangedResults() { return changedResults; }
    public void setChangedResults(int value) { changedResults = value; }
    public int getChangedMajorItems() { return changedMajorItems; }
    public void setChangedMajorItems(int value) { changedMajorItems = value; }
    public int getChangedRemarkItems() { return changedRemarkItems; }
    public void setChangedRemarkItems(int value) { changedRemarkItems = value; }
    public Map<String, Integer> getTransitions() { return transitions; }
}
