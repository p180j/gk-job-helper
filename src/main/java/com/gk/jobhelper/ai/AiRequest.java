package com.gk.jobhelper.ai;

import java.util.ArrayList;
import java.util.List;

public class AiRequest {
    private List<AiMessage> messages = new ArrayList<>();

    public List<AiMessage> getMessages() { return messages; }
    public void setMessages(List<AiMessage> messages) { this.messages = messages == null ? new ArrayList<AiMessage>() : messages; }
}
