package com.gk.jobhelper.ai;

public interface AiClient {
    String provider();
    AiResponse chat(AiProviderConfig config, AiRequest request);
}
