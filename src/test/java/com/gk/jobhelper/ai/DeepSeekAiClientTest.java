package com.gk.jobhelper.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeepSeekAiClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void chatShouldCallCompatibleEndpointAndParseContent() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> path = new AtomicReference<>();
        server = start(exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            path.set(exchange.getRequestURI().getPath());
            respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"OK\"}}]}");
        });

        AiResponse response = new DeepSeekAiClient(new ObjectMapper()).chat(config("test-key"), request());

        assertEquals("OK", response.getContent());
        assertEquals("Bearer test-key", authorization.get());
        assertEquals("/chat/completions", path.get());
    }

    @Test
    void errorShouldNotExposeApiKey() throws Exception {
        server = start(exchange -> respond(exchange, 401, "{\"error\":{\"message\":\"invalid key\"}}"));

        AiClientException exception = assertThrows(AiClientException.class,
                () -> new DeepSeekAiClient(new ObjectMapper()).chat(config("sensitive-test-key"), request()));

        assertFalse(exception.getMessage().contains("sensitive-test-key"));
        assertEquals("DeepSeek 请求失败（HTTP 401）：invalid key", exception.getMessage());
    }

    private HttpServer start(Handler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/chat/completions", handler::handle);
        httpServer.start();
        return httpServer;
    }

    private AiProviderConfig config(String apiKey) {
        AiProviderConfig config = new AiProviderConfig();
        config.setProvider("DEEPSEEK");
        config.setModel("deepseek-chat");
        config.setApiKey(apiKey);
        config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        return config;
    }

    private AiRequest request() {
        AiRequest request = new AiRequest();
        request.setMessages(Collections.singletonList(new AiMessage("user", "ping")));
        return request;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
