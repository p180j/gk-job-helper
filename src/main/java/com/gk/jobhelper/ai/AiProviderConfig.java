package com.gk.jobhelper.ai;

import javax.validation.constraints.NotBlank;

public class AiProviderConfig {
    @NotBlank(message = "provider 不能为空")
    private String provider;
    @NotBlank(message = "model 不能为空")
    private String model;
    @NotBlank(message = "apiKey 不能为空")
    private String apiKey;
    @NotBlank(message = "baseUrl 不能为空")
    private String baseUrl;
    private Integer timeoutMs = 15000;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
}
