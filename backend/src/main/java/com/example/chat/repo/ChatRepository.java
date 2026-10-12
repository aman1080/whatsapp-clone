package com.example.chat.repo;
import com.example.chat.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ChatRepository extends JpaRepository<Chat,Long> {}
