package com.example.chat.dto;
import jakarta.validation.constraints.*;
public final class AuthDtos { private AuthDtos(){} public record RegisterRequest(@NotBlank @Size(max=80) String displayName,@Email @NotBlank String email,@NotBlank @Size(min=8,max=72) String password){} public record LoginRequest(@Email @NotBlank String email,@NotBlank String password){} public record AuthResponse(String token, Long userId, String displayName, String email){} }
