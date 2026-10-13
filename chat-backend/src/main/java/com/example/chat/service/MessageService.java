package com.example.chat.service;

import com.example.chat.dto.MessageResponse;
import com.example.chat.dto.SendMessageRequest;
import com.example.chat.model.AppUser;
import com.example.chat.model.Conversation;
import com.example.chat.model.Message;
import com.example.chat.repo.ConversationParticipantRepository;
import com.example.chat.repo.ConversationRepository;
import com.example.chat.repo.MessageRepository;
import com.example.chat.repo.UserRepository;
import com.example.chat.model.ConversationParticipant;
import org.springframework.data.domain.PageRequest;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;

    public MessageService(
            MessageRepository messageRepository,
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MessageResponse sendMessage(
            Long conversationId,
            String senderEmail,
            SendMessageRequest request) {

        AppUser sender = userRepository.findByEmailIgnoreCase(senderEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        boolean isParticipant =
                participantRepository.existsByConversationIdAndUserId(
                        conversationId, sender.getId());

        if (!isParticipant) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not a participant in this conversation");
        }

        Conversation conversation = conversationRepository
                .findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conversation not found"));

        Message message = new Message(
                conversation,
                sender,
                request.content().trim());

        Message saved = messageRepository.save(message);

        return new MessageResponse(
                saved.getId(),
                conversationId,
                sender.getId(),
                sender.getDisplayName(),
                saved.getContent(),
                saved.getSentAt());
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(
            Long conversationId,
            String userEmail,
            int page,
            int size) {

        AppUser user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"));

        boolean isParticipant =
                participantRepository.existsByConversationIdAndUserId(
                        conversationId, user.getId());

        if (!isParticipant) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not a participant in this conversation");
        }

        if (!conversationRepository.existsById(conversationId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Conversation not found");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));

        return messageRepository
                .findByConversationIdOrderBySentAtDesc(
                        conversationId,
                        PageRequest.of(safePage, safeSize))
                .stream()
                .map(message -> new MessageResponse(
                        message.getId(),
                        conversationId,
                        message.getSender().getId(),
                        message.getSender().getDisplayName(),
                        message.getContent(),
                        message.getSentAt()))
                .toList();
    }
}
