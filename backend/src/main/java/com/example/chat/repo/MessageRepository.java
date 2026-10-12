package com.example.chat.repo;
import com.example.chat.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MessageRepository extends JpaRepository<ChatMessage,Long> { List<ChatMessage> findByChatIdOrderBySentAtDesc(Long chatId, Pageable pageable); }
