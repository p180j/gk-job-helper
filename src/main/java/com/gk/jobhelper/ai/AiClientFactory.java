package com.gk.jobhelper.ai;

import com.gk.jobhelper.common.ApiResponse;
import com.gk.jobhelper.common.BusinessException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AiClientFactory {
    private final Map<String, AiClient> clients = new HashMap<>();

    public AiClientFactory(List<AiClient> allClients) {
        for (AiClient client : allClients) {
            clients.put(client.provider().toUpperCase(Locale.ROOT), client);
        }
    }

    public AiClient get(String provider) {
        AiClient client = provider == null ? null : clients.get(provider.trim().toUpperCase(Locale.ROOT));
        if (client == null) {
            throw new BusinessException(ApiResponse.CODE_BAD_REQUEST, "不支持的 AI 服务商: " + provider);
        }
        return client;
    }
}
