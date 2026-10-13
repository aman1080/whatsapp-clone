package com.example.chat.controller;

import com.example.chat.model.AppUser;
import com.example.chat.repo.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getUsers(
            Authentication authentication) {

        String currentEmail = authentication.getName();

        List<Map<String, Object>> users = userRepository.findAll()
                .stream()
                .filter(user -> !user.getEmail().equalsIgnoreCase(currentEmail))
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "displayName", user.getDisplayName(),
                        "email", user.getEmail()
                ))
                .toList();

        return ResponseEntity.ok(users);
    }
}
