package com.example.chat.security;

import com.example.chat.model.AppUser;
import com.example.chat.repo.ConversationParticipantRepository;
import com.example.chat.repo.UserRepository;

import io.jsonwebtoken.JwtException;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ConversationParticipantRepository participantRepository;

    public WebSocketAuthInterceptor(
            JwtService jwtService,
            UserRepository userRepository,
            ConversationParticipantRepository participantRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.participantRepository = participantRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        // 1. Authenticate the WebSocket connection using JWT
        if (command == StompCommand.CONNECT) {

            String header =
                    accessor.getFirstNativeHeader("Authorization");

            if (header == null || !header.startsWith("Bearer ")) {
                throw new MessageDeliveryException(
                        message, "Missing WebSocket JWT");
            }

            try {
                String email =
                        jwtService.extractEmail(header.substring(7));

                AppUser user = userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new MessageDeliveryException(
                                message, "User not found"));

                Principal principal =
                        new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                null,
                                Collections.emptyList());

                accessor.setUser(principal);

            } catch (JwtException | IllegalArgumentException ex) {
                throw new MessageDeliveryException(
                        message, "Invalid or expired WebSocket JWT", ex);
            }
        }

        // 2. Validate subscriptions
        if (command == StompCommand.SUBSCRIBE) {

            Principal principal = accessor.getUser();
            String destination = accessor.getDestination();

            if (principal == null) {
                throw new MessageDeliveryException(
                        message, "WebSocket authentication required");
            }

            // Allow authenticated users to subscribe to their user queue
            if ("/user/queue/messages".equals(destination)) {
                return message;
            }

            // Conversation topic format: /topic/chat/{conversationId}
            if (destination == null
                    || !destination.matches("/topic/chat/[0-9]+")) {

                throw new MessageDeliveryException(
                        message, "Subscription destination not allowed");
            }

            Long conversationId = Long.valueOf(
                    destination.substring("/topic/chat/".length()));

            AppUser user = userRepository
                    .findByEmailIgnoreCase(principal.getName())
                    .orElseThrow(() -> new MessageDeliveryException(
                            message, "User not found"));

            boolean participant = participantRepository
                    .existsByConversationIdAndUserId(
                            conversationId, user.getId());

            if (!participant) {
                throw new MessageDeliveryException(
                        message, "You cannot access this conversation");
            }
        }

        // 3. Validate message sends
        if (command == StompCommand.SEND) {

            Principal principal = accessor.getUser();
            String destination = accessor.getDestination();

            if (principal == null) {
                throw new MessageDeliveryException(
                        message, "WebSocket authentication required");
            }

            // Frontend sends to /app/chat.send
            if (!"/app/chat.send".equals(destination)) {
                throw new MessageDeliveryException(
                        message, "Invalid chat destination");
            }
        }

        return message;
    }
}