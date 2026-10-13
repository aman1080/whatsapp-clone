package com.example.chat.dto;

import java.time.LocalDateTime;

public record MessageResponse(
        Long messageId,
        Long conversationId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime sentAt
) {}
