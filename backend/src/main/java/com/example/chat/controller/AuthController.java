package com.example.chat.controller;

import com.example.chat.dto.AuthDtos.*; import com.example.chat.model.AppUser; import com.example.chat.repo.UserRepository; import com.example.chat.security.JwtService;
import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final UserRepository users; private final PasswordEncoder encoder; private final JwtService jwt;
 public AuthController(UserRepository users,PasswordEncoder encoder,JwtService jwt){this.users=users;this.encoder=encoder;this.jwt=jwt;}
 @PostMapping("/register") public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest r){if(users.existsByEmailIgnoreCase(r.email()))return ResponseEntity.status(HttpStatus.CONFLICT).build(); AppUser u=users.save(new AppUser(r.displayName().trim(),r.email().trim(),encoder.encode(r.password()))); return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(jwt.generate(u.getEmail()),u.getId(),u.getDisplayName(),u.getEmail()));}
 @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest r){AppUser u=users.findByEmailIgnoreCase(r.email()).orElseThrow(()->new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials")); if(!encoder.matches(r.password(),u.getPasswordHash()))throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid credentials"); return new AuthResponse(jwt.generate(u.getEmail()),u.getId(),u.getDisplayName(),u.getEmail());}
}
