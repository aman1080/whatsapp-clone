package com.example.chat.controller;

import com.example.chat.dto.MessageResponse;
import com.example.chat.dto.SendMessageRequest;
import com.example.chat.service.MessageService;

import jakarta.validation.Valid;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.handler.annotation.Payload;

import java.security.Principal;
import java.util.Map;

@Controller
public class ChatWebSocketController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(
            MessageService messageService,
            SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/chat.send")
    public void sendMessage(
            @Valid @Payload Map<String, Object> payload,
            Principal principal) {

        System.out.println("WebSocket message handler called");

        System.out.println("Authenticated user: "
                + (principal != null ? principal.getName() : "null"));

        System.out.println("Payload: " + payload);

        if (principal == null) {
            throw new IllegalStateException(
                    "WebSocket user is not authenticated");
        }

        Long conversationId = Long.valueOf(
                payload.get("chatId").toString());

        String content = payload.get("content").toString();

        SendMessageRequest request = new SendMessageRequest(content);



        MessageResponse response = messageService.sendMessage(
                conversationId,
                principal.getName(),
                request);

        messagingTemplate.convertAndSend(
                "/topic/chat/" + conversationId,
                response);
        System.out.println("Message saved: " + response);

        String destination = "/topic/chat/" + conversationId;

        System.out.println("Broadcasting to: " + destination);
        System.out.println("Broadcast sent");
    }
}
