package com.example.chat.repo;
import com.example.chat.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ChatMemberRepository extends JpaRepository<ChatMember,Long> {
 List<ChatMember> findByUserId(Long userId);
 List<ChatMember> findByChatId(Long chatId);
 boolean existsByChatIdAndUserId(Long chatId, Long userId);
}
