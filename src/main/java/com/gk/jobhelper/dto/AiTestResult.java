package com.gk.jobhelper.dto;

public class AiTestResult {
    private final boolean success;
    private final String message;

    public AiTestResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
