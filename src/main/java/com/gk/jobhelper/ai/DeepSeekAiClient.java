package com.gk.jobhelper.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Component
public class DeepSeekAiClient implements AiClient {
    private static final String PROVIDER = "DEEPSEEK";
    private final ObjectMapper objectMapper;

    public DeepSeekAiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String provider() {
        return PROVIDER;
    }

    @Override
    public AiResponse chat(AiProviderConfig config, AiRequest request) {
        validate(config, request);
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(completionUrl(config.getBaseUrl())).openConnection();
            int timeout = safeTimeout(config.getTimeoutMs());
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
            byte[] body = objectMapper.writeValueAsBytes(payload(config, request));
            try (OutputStream output = connection.getOutputStream()) {
                output.write(body);
            }
            int status = connection.getResponseCode();
            String responseBody = read(status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream());
            if (status < 200 || status >= 300) {
                throw new AiClientException("DeepSeek 请求失败（HTTP " + status + "）：" + providerMessage(responseBody));
            }
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (!content.isTextual() || content.asText().trim().isEmpty()) {
                throw new AiClientException("DeepSeek 返回格式异常：未找到回复内容");
            }
            return new AiResponse(content.asText());
        } catch (AiClientException e) {
            throw e;
        } catch (IOException e) {
            throw new AiClientException("无法连接 DeepSeek：" + safeNetworkMessage(e), e);
        } catch (RuntimeException e) {
            throw new AiClientException("DeepSeek 响应解析失败", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private ObjectNode payload(AiProviderConfig config, AiRequest request) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getModel().trim());
        body.put("stream", false);
        ArrayNode messages = body.putArray("messages");
        for (AiMessage message : request.getMessages()) {
            ObjectNode item = messages.addObject();
            item.put("role", message.getRole());
            item.put("content", message.getContent());
        }
        return body;
    }

    private void validate(AiProviderConfig config, AiRequest request) {
        if (config == null || request == null || request.getMessages().isEmpty()) {
            throw new AiClientException("AI 请求参数不完整");
        }
        try {
            URI uri = URI.create(config.getBaseUrl().trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException();
            }
        } catch (Exception e) {
            throw new AiClientException("baseUrl 必须是有效的 HTTP 或 HTTPS 地址");
        }
    }

    private String completionUrl(String baseUrl) {
        return baseUrl.trim().replaceAll("/+$", "") + "/chat/completions";
    }

    private int safeTimeout(Integer timeoutMs) {
        return timeoutMs == null ? 15000 : Math.max(3000, Math.min(timeoutMs, 60000));
    }

    private String read(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && text.length() < 4096) {
                text.append(line);
            }
            return text.toString();
        }
    }

    private String providerMessage(String body) {
        try {
            String message = objectMapper.readTree(body).path("error").path("message").asText();
            return message.isEmpty() ? "服务端未提供详细原因" : message;
        } catch (Exception ignored) {
            return "服务端返回异常";
        }
    }

    private String safeNetworkMessage(IOException exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? "网络连接失败" : message.replaceAll("(?i)Bearer\\s+[^\\s]+", "Bearer ***");
    }
}
