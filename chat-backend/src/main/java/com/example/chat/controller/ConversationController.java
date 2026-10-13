package com.example.chat.controller;

import com.example.chat.model.AppUser;
import com.example.chat.repo.UserRepository;
import com.example.chat.service.ConversationService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;
    private final UserRepository userRepository;

    public ConversationController(
            ConversationService conversationService,
            UserRepository userRepository) {
        this.conversationService = conversationService;
        this.userRepository = userRepository;
    }

    @PostMapping("/direct/{otherUserId}")
    public ResponseEntity<Map<String, Object>> openDirectConversation(
            @PathVariable Long otherUserId,
            Authentication authentication) {

        AppUser currentUser = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException("Authenticated user not found"));

        Long conversationId = conversationService.openDirectConversation(
                currentUser.getId(),
                otherUserId
        );

        return ResponseEntity.ok(Map.of(
                "conversationId", conversationId,
                "message", "Conversation opened successfully"
        ));
    }


    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMyConversations(
            Authentication authentication) {

        AppUser currentUser = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found"));

        List<Map<String, Object>> conversations =
                conversationService.getConversationsForUser(currentUser.getId());

        return ResponseEntity.ok(conversations);
    }

}
