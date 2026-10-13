package com.example.chat.repo;

import com.example.chat.model.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationParticipantRepository
        extends JpaRepository<ConversationParticipant, Long> {

    List<ConversationParticipant> findByUserId(Long userId);

    List<ConversationParticipant> findByConversationId(Long conversationId);

    boolean existsByConversationIdAndUserId(
            Long conversationId,
            Long userId
    );

    @Query("""
    SELECT cp.conversation.id
    FROM ConversationParticipant cp
    WHERE cp.user.id IN (:userId1, :userId2)
    GROUP BY cp.conversation.id
    HAVING COUNT(DISTINCT cp.user.id) = 2
       AND COUNT(cp.id) = 2
    """)
    Optional<Long> findDirectConversationId(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2
    );
}
