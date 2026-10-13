package com.example.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Message content cannot be empty")
        @Size(max = 5000, message = "Message cannot exceed 5000 characters")
        String content
) {}
