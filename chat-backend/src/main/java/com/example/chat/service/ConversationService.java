package com.example.chat.service;

import com.example.chat.model.AppUser;
import com.example.chat.model.Conversation;
import com.example.chat.model.ConversationParticipant;
import com.example.chat.repo.ConversationParticipantRepository;
import com.example.chat.repo.ConversationRepository;
import com.example.chat.repo.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final UserRepository userRepository;

    public ConversationService(
            ConversationRepository conversationRepository,
            ConversationParticipantRepository participantRepository,
            UserRepository userRepository) {
        this.conversationRepository = conversationRepository;
        this.participantRepository = participantRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Long openDirectConversation(Long currentUserId, Long otherUserId) {

        if (currentUserId.equals(otherUserId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot start a conversation with yourself"
            );
        }

        AppUser currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Current user not found"));

        AppUser otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Other user not found"));

        var existingId = participantRepository.findDirectConversationId(
                currentUserId, otherUserId);

        if (existingId.isPresent()) {
            return existingId.get();
        }

        Conversation conversation = conversationRepository.save(
                new Conversation());

        participantRepository.save(
                new ConversationParticipant(conversation, currentUser));

        participantRepository.save(
                new ConversationParticipant(conversation, otherUser));

        return conversation.getId();
    }


    @Transactional(readOnly = true)
    public List<Map<String, Object>> getConversationsForUser(Long userId) {

        return participantRepository.findByUserId(userId)
                .stream()
                .map(participant -> {
                    Conversation conversation = participant.getConversation();

                    return Map.<String, Object>of(
                            "id", conversation.getId(),
                            "type", "DIRECT",
                            "createdAt", conversation.getCreatedAt()
                    );
                })
                .toList();
    }

}
