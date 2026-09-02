package com.gk.jobhelper.service;

import com.gk.jobhelper.ai.AiClientException;
import com.gk.jobhelper.ai.AiClientFactory;
import com.gk.jobhelper.ai.AiMessage;
import com.gk.jobhelper.ai.AiProviderConfig;
import com.gk.jobhelper.ai.AiRequest;
import com.gk.jobhelper.dto.AiTestResult;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class AiTestService {
    private final AiClientFactory aiClientFactory;

    public AiTestService(AiClientFactory aiClientFactory) {
        this.aiClientFactory = aiClientFactory;
    }

    public AiTestResult test(AiProviderConfig config) {
        try {
            AiRequest request = new AiRequest();
            request.setMessages(Arrays.asList(
                    new AiMessage("system", "You are a connectivity test service."),
                    new AiMessage("user", "Reply only with OK.")
            ));
            aiClientFactory.get(config.getProvider()).chat(config, request);
            return new AiTestResult(true, "连接成功");
        } catch (AiClientException e) {
            return new AiTestResult(false, e.getMessage());
        }
    }
}
