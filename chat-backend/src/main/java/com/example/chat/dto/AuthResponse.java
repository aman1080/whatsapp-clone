package com.example.chat.dto;

public record AuthResponse(
        Long userId,
        String displayName,
        String email,
        String token
) {}
